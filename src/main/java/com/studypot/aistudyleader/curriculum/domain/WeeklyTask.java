package com.studypot.aistudyleader.curriculum.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record WeeklyTask(
	UUID id,
	UUID curriculumWeekId,
	int displayOrder,
	WeeklyTaskType taskType,
	String title,
	String description,
	boolean required,
	Instant dueAt,
	boolean generatedByAi,
	Map<String, Object> sourcePayload,
	Instant createdAt,
	Instant updatedAt
) {

	public WeeklyTask {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(curriculumWeekId, "curriculumWeekId must not be null");
		if (displayOrder <= 0) {
			throw new IllegalArgumentException("displayOrder must be positive");
		}
		Objects.requireNonNull(taskType, "taskType must not be null");
		title = requireText(title, "title");
		description = description == null || description.isBlank() ? null : description.strip();
		sourcePayload = Map.copyOf(Objects.requireNonNull(sourcePayload, "sourcePayload must not be null"));
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.strip();
	}
}
