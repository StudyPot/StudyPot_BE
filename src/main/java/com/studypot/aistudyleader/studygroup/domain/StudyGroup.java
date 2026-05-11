package com.studypot.aistudyleader.studygroup.domain;

import com.studypot.aistudyleader.global.domain.AggregateRoot;
import com.studypot.aistudyleader.global.domain.AuditMetadata;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class StudyGroup extends AggregateRoot<UUID> {

	private static final int MAX_NAME_LENGTH = 120;
	private static final int MAX_TOPIC_LENGTH = 120;
	private static final int MAX_INVITE_CODE_LENGTH = 80;

	private final UUID createdBy;
	private final String name;
	private final String description;
	private final String topic;
	private final List<String> detailKeywords;
	private final StudyGroupStatus status;
	private final int maxMembers;
	private final boolean isPublic;
	private final String inviteCode;
	private final LocalDate startsAt;
	private final LocalDate endsAt;
	private final Instant onboardingStartedAt;
	private final Instant startedAt;
	private final AuditMetadata auditMetadata;

	private StudyGroup(
		UUID id,
		UUID createdBy,
		String name,
		String topic,
		List<String> detailKeywords,
		StudyGroupStatus status,
		int maxMembers,
		boolean isPublic,
		String inviteCode,
		LocalDate startsAt,
		LocalDate endsAt,
		String description,
		Instant onboardingStartedAt,
		Instant startedAt,
		AuditMetadata auditMetadata
	) {
		super(id);
		this.createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
		this.name = requireText(name, "study group name", MAX_NAME_LENGTH);
		this.topic = requireText(topic, "study group topic", MAX_TOPIC_LENGTH);
		this.detailKeywords = normalizeDetailKeywords(detailKeywords);
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.maxMembers = requirePositive(maxMembers);
		this.isPublic = isPublic;
		this.inviteCode = requireText(inviteCode, "inviteCode", MAX_INVITE_CODE_LENGTH);
		this.startsAt = Objects.requireNonNull(startsAt, "startsAt must not be null");
		this.endsAt = Objects.requireNonNull(endsAt, "endsAt must not be null");
		if (endsAt.isBefore(startsAt)) {
			throw new IllegalArgumentException("endsAt must be on or after startsAt");
		}
		this.description = blankToNull(description);
		this.onboardingStartedAt = onboardingStartedAt;
		this.startedAt = startedAt;
		this.auditMetadata = Objects.requireNonNull(auditMetadata, "auditMetadata must not be null");
	}

	public static StudyGroup create(
		UUID id,
		UUID createdBy,
		String name,
		String topic,
		List<String> detailKeywords,
		int maxMembers,
		LocalDate startsAt,
		LocalDate endsAt,
		String description,
		String inviteCode,
		Instant now
	) {
		Objects.requireNonNull(now, "now must not be null");
		return new StudyGroup(
			id,
			createdBy,
			name,
			topic,
			detailKeywords,
			StudyGroupStatus.ONBOARDING,
			maxMembers,
			false,
			inviteCode,
			startsAt,
			endsAt,
			description,
			now,
			null,
			AuditMetadata.created(now)
		);
	}

	public static StudyGroup rehydrate(
		UUID id,
		UUID createdBy,
		String name,
		String topic,
		List<String> detailKeywords,
		StudyGroupStatus status,
		int maxMembers,
		boolean isPublic,
		String inviteCode,
		LocalDate startsAt,
		LocalDate endsAt,
		String description,
		Instant onboardingStartedAt,
		Instant startedAt,
		Instant createdAt,
		Instant updatedAt
	) {
		return new StudyGroup(
			id,
			createdBy,
			name,
			topic,
			detailKeywords,
			status,
			maxMembers,
			isPublic,
			inviteCode,
			startsAt,
			endsAt,
			description,
			onboardingStartedAt,
			startedAt,
			new AuditMetadata(createdAt, updatedAt, null)
		);
	}

	public UUID createdBy() {
		return createdBy;
	}

	public String name() {
		return name;
	}

	public Optional<String> description() {
		return Optional.ofNullable(description);
	}

	public String topic() {
		return topic;
	}

	public List<String> detailKeywords() {
		return detailKeywords;
	}

	public StudyGroupStatus status() {
		return status;
	}

	public int maxMembers() {
		return maxMembers;
	}

	public boolean isPublic() {
		return isPublic;
	}

	public String inviteCode() {
		return inviteCode;
	}

	public LocalDate startsAt() {
		return startsAt;
	}

	public LocalDate endsAt() {
		return endsAt;
	}

	public Optional<Instant> onboardingStartedAt() {
		return Optional.ofNullable(onboardingStartedAt);
	}

	public Optional<Instant> startedAt() {
		return Optional.ofNullable(startedAt);
	}

	public AuditMetadata auditMetadata() {
		return auditMetadata;
	}

	private static String requireText(String value, String fieldName, int maxLength) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		String normalized = value.strip();
		if (normalized.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " length must be <= " + maxLength + ": " + normalized.length());
		}
		return normalized;
	}

	private static List<String> normalizeDetailKeywords(List<String> values) {
		if (values == null || values.isEmpty()) {
			throw new IllegalArgumentException("detailKeywords must not be empty");
		}
		List<String> normalized = values.stream()
			.map(value -> {
				if (value == null || value.isBlank()) {
					throw new IllegalArgumentException("detailKeywords must not contain blank values");
				}
				return value.strip();
			})
			.toList();
		return List.copyOf(normalized);
	}

	private static int requirePositive(int value) {
		if (value <= 0) {
			throw new IllegalArgumentException("maxMembers must be positive");
		}
		return value;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}
