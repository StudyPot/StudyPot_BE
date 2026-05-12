package com.studypot.aistudyleader.studygroup.rules.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record RuleViolation(
	UUID id,
	UUID ruleId,
	UUID memberId,
	Optional<UUID> taskCompletionId,
	RuleViolationType violationType,
	Map<String, Object> details,
	RuleViolationStatus status,
	Optional<Instant> resolvedAt,
	Optional<String> resolvedNote,
	Instant occurredAt,
	Instant createdAt
) {

	public RuleViolation {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(ruleId, "ruleId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		taskCompletionId = Objects.requireNonNull(taskCompletionId, "taskCompletionId must not be null");
		Objects.requireNonNull(violationType, "violationType must not be null");
		details = Map.copyOf(Objects.requireNonNull(details, "details must not be null"));
		Objects.requireNonNull(status, "status must not be null");
		resolvedAt = Objects.requireNonNull(resolvedAt, "resolvedAt must not be null");
		resolvedNote = Objects.requireNonNull(resolvedNote, "resolvedNote must not be null")
			.map(RuleViolation::normalize)
			.filter(value -> !value.isBlank());
		Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
	}

	public static RuleViolation open(
		UUID id,
		UUID ruleId,
		UUID memberId,
		UUID taskCompletionId,
		RuleViolationType violationType,
		Map<String, Object> details,
		Instant occurredAt,
		Instant now
	) {
		return new RuleViolation(
			id,
			ruleId,
			memberId,
			Optional.ofNullable(taskCompletionId),
			violationType,
			details,
			RuleViolationStatus.OPEN,
			Optional.empty(),
			Optional.empty(),
			occurredAt,
			now
		);
	}

	public boolean isOpen() {
		return status == RuleViolationStatus.OPEN;
	}

	public RuleViolation resolve(String note, Instant now) {
		return handle(RuleViolationStatus.RESOLVED, note, now);
	}

	public RuleViolation waive(String note, Instant now) {
		return handle(RuleViolationStatus.WAIVED, note, now);
	}

	private RuleViolation handle(RuleViolationStatus nextStatus, String note, Instant now) {
		return new RuleViolation(
			id,
			ruleId,
			memberId,
			taskCompletionId,
			violationType,
			details,
			nextStatus,
			Optional.of(now),
			Optional.ofNullable(note),
			occurredAt,
			createdAt
		);
	}

	private static String normalize(String value) {
		return value == null ? null : value.strip();
	}
}
