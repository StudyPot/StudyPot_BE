package com.studypot.aistudyleader.retrospective.domain;

import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgressStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RetrospectiveProgress(
	UUID id,
	UUID curriculumWeekId,
	UUID memberId,
	MemberWeekProgressStatus status,
	Instant startedAt,
	Instant dueAt,
	Instant completedAt,
	String completionNote,
	String incompleteReason,
	Instant reasonSubmittedAt
) {

	public RetrospectiveProgress {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(curriculumWeekId, "curriculumWeekId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(dueAt, "dueAt must not be null");
		completionNote = normalizeNullableText(completionNote);
		incompleteReason = normalizeNullableText(incompleteReason);
	}

	private static String normalizeNullableText(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}
}
