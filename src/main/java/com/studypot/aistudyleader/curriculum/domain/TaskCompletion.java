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
		if (!canTransitionTo(requestedStatus)) {
			throw new IllegalArgumentException("task completion cannot transition from " + status + " to " + requestedStatus + ".");
		}

		String nextCompletionNote = normalizeNullableText(requestedCompletionNote);
		String nextIncompleteReason = normalizeNullableText(requestedIncompleteReason);
		String nextEvidenceUrl = normalizeNullableText(requestedEvidenceUrl);
		return switch (requestedStatus) {
			// 체크 해제: 완료 기록을 비우고 초기(TODO) 상태로 되돌린다(완료 ↔ 미체크 토글).
			case TODO -> todo(now);
			case DONE -> done(now, nextCompletionNote, nextIncompleteReason, nextEvidenceUrl);
			case INCOMPLETE -> incomplete(now, nextCompletionNote, nextIncompleteReason, nextEvidenceUrl);
			case SKIPPED -> skipped(now, nextCompletionNote, nextIncompleteReason, nextEvidenceUrl);
		};
	}

	private TaskCompletion todo(Instant now) {
		// 모든 완료/미완료 부가정보를 비운다.
		return copy(TaskCompletionStatus.TODO, null, null, null, null, null, now);
	}

	private TaskCompletion done(Instant now, String requestedCompletionNote, String requestedIncompleteReason, String requestedEvidenceUrl) {
		if (requestedIncompleteReason != null) {
			throw new IllegalArgumentException("incomplete reason is not allowed when status is DONE.");
		}
		// 마감 경과 여부와 무관하게 완료를 허용한다(완료/미완료 자유 토글). 이미 완료된 경우 첫 완료 정보를 유지한다.
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
		// 미완료 사유는 선택값이다. 마감 전/후, 완료 상태에서도 미완료로 자유롭게 전환할 수 있다.
		String nextIncompleteReason = requestedIncompleteReason != null ? requestedIncompleteReason : incompleteReason;
		Instant nextReasonSubmittedAt = nextIncompleteReason == null
			? null
			: (reasonSubmittedAt == null ? now : reasonSubmittedAt);
		return copy(
			TaskCompletionStatus.INCOMPLETE,
			null,
			null,
			nextIncompleteReason,
			nextReasonSubmittedAt,
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
		// 완료/미완료/스킵/TODO(체크 해제) 사이를 자유롭게 전환할 수 있다.
		return true;
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
