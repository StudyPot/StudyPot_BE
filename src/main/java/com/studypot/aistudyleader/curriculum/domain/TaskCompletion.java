package com.studypot.aistudyleader.curriculum.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TaskCompletion(
	UUID id,
	UUID progressId,
	UUID weeklyTaskId,
	UUID memberId,
	TaskCompletionStatus status,
	Instant dueAt,
	Instant completedAt,
	String completionNote,
	String incompleteReason,
	Instant reasonSubmittedAt,
	String evidenceUrl,
	Instant createdAt,
	Instant updatedAt
) {

	public TaskCompletion {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(progressId, "progressId must not be null");
		Objects.requireNonNull(weeklyTaskId, "weeklyTaskId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		completionNote = normalizeNullableText(completionNote);
		incompleteReason = normalizeNullableText(incompleteReason);
		evidenceUrl = normalizeNullableText(evidenceUrl);
	}

	public static TaskCompletion create(
		UUID id,
		UUID progressId,
		UUID weeklyTaskId,
		UUID memberId,
		Instant dueAt,
		TaskCompletionStatus requestedStatus,
		String completionNote,
		String incompleteReason,
		String evidenceUrl,
		Instant now
	) {
		TaskCompletion completion = new TaskCompletion(
			id,
			progressId,
			weeklyTaskId,
			memberId,
			TaskCompletionStatus.TODO,
			dueAt,
			null,
			null,
			null,
			null,
			null,
			now,
			now
		);
		return completion.update(requestedStatus, completionNote, incompleteReason, evidenceUrl, now);
	}

	public TaskCompletion update(
		TaskCompletionStatus requestedStatus,
		String requestedCompletionNote,
		String requestedIncompleteReason,
		String requestedEvidenceUrl,
		Instant now
	) {
		Objects.requireNonNull(now, "now must not be null");
		requireNotBeforeExistingTimestamps(now);
		if (requestedStatus == null) {
			throw new IllegalArgumentException("status is required.");
		}
		if (requestedStatus == TaskCompletionStatus.TODO) {
			throw new IllegalArgumentException("TODO is not a task completion action.");
		}
		if (!canTransitionTo(requestedStatus)) {
			throw new IllegalArgumentException("task completion cannot transition from " + status + " to " + requestedStatus + ".");
		}

		String nextCompletionNote = normalizeNullableText(requestedCompletionNote);
		String nextIncompleteReason = normalizeNullableText(requestedIncompleteReason);
		String nextEvidenceUrl = normalizeNullableText(requestedEvidenceUrl);
		return switch (requestedStatus) {
			case TODO -> throw new IllegalArgumentException("TODO is not a task completion action.");
			case DONE -> done(now, nextCompletionNote, nextIncompleteReason, nextEvidenceUrl);
			case INCOMPLETE -> incomplete(now, nextCompletionNote, nextIncompleteReason, nextEvidenceUrl);
			case SKIPPED -> skipped(now, nextCompletionNote, nextIncompleteReason, nextEvidenceUrl);
		};
	}

	private TaskCompletion done(Instant now, String requestedCompletionNote, String requestedIncompleteReason, String requestedEvidenceUrl) {
		if (requestedIncompleteReason != null) {
			throw new IllegalArgumentException("incomplete reason is not allowed when status is DONE.");
		}
		if (completedAt == null && dueAt != null && now.isAfter(dueAt)) {
			throw new IllegalArgumentException("task is overdue; submit incomplete reason.");
		}
		Instant nextCompletedAt = completedAt == null ? now : completedAt;
		String nextCompletionNote = completedAt == null ? requestedCompletionNote : completionNote;
		String nextEvidenceUrl = completedAt == null ? requestedEvidenceUrl : evidenceUrl;
		return copy(TaskCompletionStatus.DONE, nextCompletedAt, nextCompletionNote, null, null, nextEvidenceUrl, now);
	}

	private TaskCompletion incomplete(Instant now, String requestedCompletionNote, String requestedIncompleteReason, String requestedEvidenceUrl) {
		if (requestedCompletionNote != null) {
			throw new IllegalArgumentException("completion note is not allowed when status is INCOMPLETE.");
		}
		if (requestedEvidenceUrl != null) {
			throw new IllegalArgumentException("evidence url is not allowed when status is INCOMPLETE.");
		}
		if (requestedIncompleteReason == null && incompleteReason == null) {
			throw new IllegalArgumentException("incomplete reason is required when status is INCOMPLETE.");
		}
		if (reasonSubmittedAt == null && dueAt != null && now.isBefore(dueAt)) {
			throw new IllegalArgumentException("task is not overdue yet.");
		}
		String nextIncompleteReason = reasonSubmittedAt == null ? requestedIncompleteReason : incompleteReason;
		return copy(
			TaskCompletionStatus.INCOMPLETE,
			null,
			null,
			nextIncompleteReason,
			reasonSubmittedAt == null ? now : reasonSubmittedAt,
			null,
			now
		);
	}

	private TaskCompletion skipped(Instant now, String requestedCompletionNote, String requestedIncompleteReason, String requestedEvidenceUrl) {
		if (requestedCompletionNote != null || requestedIncompleteReason != null || requestedEvidenceUrl != null) {
			throw new IllegalArgumentException("SKIPPED task completion cannot include completion or incomplete fields.");
		}
		return copy(TaskCompletionStatus.SKIPPED, null, null, null, null, null, now);
	}

	private boolean canTransitionTo(TaskCompletionStatus nextStatus) {
		if (status == nextStatus) {
			return true;
		}
		return status == TaskCompletionStatus.TODO && nextStatus != TaskCompletionStatus.TODO;
	}

	private void requireNotBeforeExistingTimestamps(Instant now) {
		requireNotBefore(now, createdAt);
		requireNotBefore(now, updatedAt);
		requireNotBefore(now, completedAt);
		requireNotBefore(now, reasonSubmittedAt);
	}

	private static void requireNotBefore(Instant now, Instant existingTimestamp) {
		if (existingTimestamp != null && now.isBefore(existingTimestamp)) {
			throw new IllegalArgumentException("now must not be before existing task completion timestamps.");
		}
	}

	private TaskCompletion copy(
		TaskCompletionStatus nextStatus,
		Instant nextCompletedAt,
		String nextCompletionNote,
		String nextIncompleteReason,
		Instant nextReasonSubmittedAt,
		String nextEvidenceUrl,
		Instant nextUpdatedAt
	) {
		return new TaskCompletion(
			id,
			progressId,
			weeklyTaskId,
			memberId,
			nextStatus,
			dueAt,
			nextCompletedAt,
			nextCompletionNote,
			nextIncompleteReason,
			nextReasonSubmittedAt,
			nextEvidenceUrl,
			createdAt,
			nextUpdatedAt
		);
	}

	private static String normalizeNullableText(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}
}
