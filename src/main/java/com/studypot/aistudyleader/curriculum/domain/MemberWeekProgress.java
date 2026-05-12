package com.studypot.aistudyleader.curriculum.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MemberWeekProgress(
	UUID id,
	UUID curriculumWeekId,
	UUID memberId,
	MemberWeekProgressStatus status,
	Instant startedAt,
	Instant dueAt,
	Instant completedAt,
	String completionNote,
	String incompleteReason,
	Instant reasonSubmittedAt,
	Instant createdAt,
	Instant updatedAt
) {

	public MemberWeekProgress {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(curriculumWeekId, "curriculumWeekId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(dueAt, "dueAt must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		completionNote = normalizeNullableText(completionNote);
		incompleteReason = normalizeNullableText(incompleteReason);
	}

	public static MemberWeekProgress create(
		UUID id,
		UUID curriculumWeekId,
		UUID memberId,
		Instant dueAt,
		MemberWeekProgressStatus status,
		String completionNote,
		String incompleteReason,
		Instant now
	) {
		MemberWeekProgress progress = new MemberWeekProgress(
			id,
			curriculumWeekId,
			memberId,
			MemberWeekProgressStatus.NOT_STARTED,
			null,
			dueAt,
			null,
			null,
			null,
			null,
			now,
			now
		);
		return progress.update(status, completionNote, incompleteReason, now);
	}

	public MemberWeekProgress update(
		MemberWeekProgressStatus requestedStatus,
		String requestedCompletionNote,
		String requestedIncompleteReason,
		Instant now
	) {
		Objects.requireNonNull(now, "now must not be null");
		MemberWeekProgressStatus nextStatus = requestedStatus == null ? status : requestedStatus;
		String nextCompletionNote = normalizeNullableText(requestedCompletionNote);
		String nextIncompleteReason = normalizeNullableText(requestedIncompleteReason);
		return switch (nextStatus) {
			case NOT_STARTED -> notStarted(now, nextCompletionNote, nextIncompleteReason);
			case IN_PROGRESS -> inProgress(now, nextCompletionNote, nextIncompleteReason);
			case COMPLETED -> completed(now, nextCompletionNote, nextIncompleteReason);
			case INCOMPLETE -> incomplete(now, nextCompletionNote, nextIncompleteReason);
			case FEEDBACK_READY -> feedbackReady(now, nextCompletionNote, nextIncompleteReason);
		};
	}

	private MemberWeekProgress notStarted(Instant now, String requestedCompletionNote, String requestedIncompleteReason) {
		if (requestedCompletionNote != null || requestedIncompleteReason != null) {
			throw new IllegalArgumentException("NOT_STARTED progress cannot include completion or incomplete fields.");
		}
		return copy(MemberWeekProgressStatus.NOT_STARTED, null, null, null, null, null, now);
	}

	private MemberWeekProgress inProgress(Instant now, String requestedCompletionNote, String requestedIncompleteReason) {
		if (requestedCompletionNote != null || requestedIncompleteReason != null) {
			throw new IllegalArgumentException("IN_PROGRESS progress cannot include completion or incomplete fields.");
		}
		return copy(MemberWeekProgressStatus.IN_PROGRESS, startedAt == null ? now : startedAt, null, null, null, null, now);
	}

	private MemberWeekProgress completed(Instant now, String requestedCompletionNote, String requestedIncompleteReason) {
		if (requestedIncompleteReason != null) {
			throw new IllegalArgumentException("incomplete reason is not allowed when status is COMPLETED.");
		}
		return copy(
			MemberWeekProgressStatus.COMPLETED,
			startedAt == null ? now : startedAt,
			now,
			requestedCompletionNote,
			null,
			null,
			now
		);
	}

	private MemberWeekProgress incomplete(Instant now, String requestedCompletionNote, String requestedIncompleteReason) {
		if (requestedCompletionNote != null) {
			throw new IllegalArgumentException("completion note is not allowed when status is INCOMPLETE.");
		}
		if (requestedIncompleteReason == null) {
			throw new IllegalArgumentException("incomplete reason is required when status is INCOMPLETE.");
		}
		return copy(
			MemberWeekProgressStatus.INCOMPLETE,
			startedAt == null ? now : startedAt,
			null,
			null,
			requestedIncompleteReason,
			now,
			now
		);
	}

	private MemberWeekProgress feedbackReady(Instant now, String requestedCompletionNote, String requestedIncompleteReason) {
		if (requestedCompletionNote != null || requestedIncompleteReason != null) {
			throw new IllegalArgumentException("FEEDBACK_READY progress cannot include completion or incomplete fields.");
		}
		if (completedAt == null && reasonSubmittedAt == null) {
			throw new IllegalArgumentException("FEEDBACK_READY progress requires completed or incomplete progress first.");
		}
		return copy(MemberWeekProgressStatus.FEEDBACK_READY, startedAt, completedAt, completionNote, incompleteReason, reasonSubmittedAt, now);
	}

	private MemberWeekProgress copy(
		MemberWeekProgressStatus nextStatus,
		Instant nextStartedAt,
		Instant nextCompletedAt,
		String nextCompletionNote,
		String nextIncompleteReason,
		Instant nextReasonSubmittedAt,
		Instant nextUpdatedAt
	) {
		return new MemberWeekProgress(
			id,
			curriculumWeekId,
			memberId,
			nextStatus,
			nextStartedAt,
			dueAt,
			nextCompletedAt,
			nextCompletionNote,
			nextIncompleteReason,
			nextReasonSubmittedAt,
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
