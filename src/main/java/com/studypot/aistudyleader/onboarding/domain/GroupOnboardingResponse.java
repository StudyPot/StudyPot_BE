package com.studypot.aistudyleader.onboarding.domain;

import com.studypot.aistudyleader.global.domain.AggregateRoot;
import com.studypot.aistudyleader.global.domain.AuditMetadata;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class GroupOnboardingResponse extends AggregateRoot<UUID> {

	private final UUID groupId;
	private final UUID memberId;
	private final Map<String, Integer> keywordSkillLevels;
	private final Map<String, Integer> taskPreferences;
	private final String additionalNote;
	private final List<MemberAvailabilitySlot> availabilitySlots;
	private final GroupOnboardingStatus status;
	private final Instant submittedAt;
	private final AuditMetadata auditMetadata;

	private GroupOnboardingResponse(
		UUID id,
		UUID groupId,
		UUID memberId,
		Map<String, Integer> keywordSkillLevels,
		Map<String, Integer> taskPreferences,
		String additionalNote,
		List<MemberAvailabilitySlot> availabilitySlots,
		GroupOnboardingStatus status,
		Instant submittedAt,
		AuditMetadata auditMetadata
	) {
		super(id);
		this.groupId = Objects.requireNonNull(groupId, "groupId must not be null");
		this.memberId = Objects.requireNonNull(memberId, "memberId must not be null");
		this.keywordSkillLevels = Map.copyOf(keywordSkillLevels);
		this.taskPreferences = Map.copyOf(taskPreferences);
		this.additionalNote = blankToNull(additionalNote);
		this.availabilitySlots = validateAvailabilitySlots(id, memberId, availabilitySlots);
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.submittedAt = submittedAt;
		this.auditMetadata = Objects.requireNonNull(auditMetadata, "auditMetadata must not be null");
	}

	public static GroupOnboardingResponse draft(
		UUID id,
		OnboardingMemberContext context,
		Map<String, Integer> keywordSkillLevels,
		Map<String, Integer> taskPreferences,
		String additionalNote,
		Instant now
	) {
		Objects.requireNonNull(context, "context must not be null");
		Objects.requireNonNull(now, "now must not be null");
		return new GroupOnboardingResponse(
			id,
			context.groupId(),
			context.memberId(),
			validateKeywordSkillLevels(context, keywordSkillLevels),
			validateTaskPreferences(taskPreferences),
			additionalNote,
			List.of(),
			GroupOnboardingStatus.DRAFT,
			null,
			AuditMetadata.created(now)
		);
	}

	public static GroupOnboardingResponse rehydrate(
		UUID id,
		UUID groupId,
		UUID memberId,
		Map<String, Integer> keywordSkillLevels,
		Map<String, Integer> taskPreferences,
		String additionalNote,
		GroupOnboardingStatus status,
		Instant submittedAt,
		Instant createdAt,
		Instant updatedAt
	) {
		return new GroupOnboardingResponse(
			id,
			groupId,
			memberId,
			Map.copyOf(Objects.requireNonNull(keywordSkillLevels, "keywordSkillLevels must not be null")),
			Map.copyOf(Objects.requireNonNull(taskPreferences, "taskPreferences must not be null")),
			additionalNote,
			List.of(),
			status,
			submittedAt,
			new AuditMetadata(createdAt, updatedAt, null)
		);
	}

	public UUID groupId() {
		return groupId;
	}

	public UUID memberId() {
		return memberId;
	}

	public Map<String, Integer> keywordSkillLevels() {
		return keywordSkillLevels;
	}

	public Map<String, Integer> taskPreferences() {
		return taskPreferences;
	}

	public Optional<String> additionalNote() {
		return Optional.ofNullable(additionalNote);
	}

	public List<MemberAvailabilitySlot> availabilitySlots() {
		return availabilitySlots;
	}

	public GroupOnboardingResponse withAvailabilitySlots(List<MemberAvailabilitySlot> slots) {
		return new GroupOnboardingResponse(
			id(),
			groupId,
			memberId,
			keywordSkillLevels,
			taskPreferences,
			additionalNote,
			slots,
			status,
			submittedAt,
			auditMetadata
		);
	}

	public GroupOnboardingStatus status() {
		return status;
	}

	public Optional<Instant> submittedAt() {
		return Optional.ofNullable(submittedAt);
	}

	public AuditMetadata auditMetadata() {
		return auditMetadata;
	}

	private static Map<String, Integer> validateKeywordSkillLevels(
		OnboardingMemberContext context,
		Map<String, Integer> values
	) {
		Map<String, Integer> normalized = normalizeMap(values, "keywordSkillLevels");
		for (var entry : normalized.entrySet()) {
			if (!context.containsKeyword(entry.getKey())) {
				throw new IllegalArgumentException("keywordSkillLevels contains unknown group keyword: " + entry.getKey());
			}
			requireScore("keywordSkillLevels", entry);
		}
		return normalized;
	}

	private static Map<String, Integer> validateTaskPreferences(Map<String, Integer> values) {
		Map<String, Integer> normalized = normalizeMap(values, "taskPreferences");
		for (var entry : normalized.entrySet()) {
			if (TaskPreferenceType.parse(entry.getKey()).isEmpty()) {
				throw new IllegalArgumentException("taskPreferences contains unknown task type: " + entry.getKey());
			}
			requireScore("taskPreferences", entry);
		}
		return normalized;
	}

	private static Map<String, Integer> normalizeMap(Map<String, Integer> values, String fieldName) {
		if (values == null) {
			throw new IllegalArgumentException(fieldName + " must not be null");
		}
		Map<String, Integer> normalized = new LinkedHashMap<>();
		for (var entry : values.entrySet()) {
			String key = entry.getKey();
			if (key == null || key.isBlank()) {
				throw new IllegalArgumentException(fieldName + " must not contain blank keys");
			}
			normalized.put(key.strip(), entry.getValue());
		}
		return Map.copyOf(normalized);
	}

	private static void requireScore(String fieldName, Map.Entry<String, Integer> entry) {
		Integer score = entry.getValue();
		if (score == null || score < 1 || score > 5) {
			throw new IllegalArgumentException(fieldName + " score must be between 1 and 5: " + entry.getKey() + "=" + score);
		}
	}

	private static List<MemberAvailabilitySlot> validateAvailabilitySlots(
		UUID responseId,
		UUID memberId,
		List<MemberAvailabilitySlot> slots
	) {
		List<MemberAvailabilitySlot> copied = List.copyOf(Objects.requireNonNull(slots, "availabilitySlots must not be null"));
		for (MemberAvailabilitySlot slot : copied) {
			if (!responseId.equals(slot.onboardingResponseId()) || !memberId.equals(slot.memberId())) {
				throw new IllegalArgumentException("availabilitySlots must belong to onboarding response");
			}
		}
		return copied;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}
