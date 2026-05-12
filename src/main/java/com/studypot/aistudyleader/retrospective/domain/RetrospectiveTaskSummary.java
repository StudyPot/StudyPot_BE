package com.studypot.aistudyleader.retrospective.domain;

import com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RetrospectiveTaskSummary(
	UUID taskId,
	int displayOrder,
	WeeklyTaskType taskType,
	String title,
	boolean required,
	Instant dueAt,
	TaskCompletionStatus status,
	Instant completedAt,
	String completionNote,
	String incompleteReason,
	Instant reasonSubmittedAt
) {

	public RetrospectiveTaskSummary {
		Objects.requireNonNull(taskId, "taskId must not be null");
		if (displayOrder < 1) {
			throw new IllegalArgumentException("displayOrder must be positive.");
		}
		Objects.requireNonNull(taskType, "taskType must not be null");
		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("title must not be blank.");
		}
		Objects.requireNonNull(dueAt, "dueAt must not be null");
		Objects.requireNonNull(status, "status must not be null");
		title = title.strip();
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
