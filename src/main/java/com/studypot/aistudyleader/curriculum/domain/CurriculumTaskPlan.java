package com.studypot.aistudyleader.curriculum.domain;

import java.util.Objects;

public record CurriculumTaskPlan(
	WeeklyTaskType taskType,
	String title,
	String description,
	boolean required
) {

	public CurriculumTaskPlan {
		Objects.requireNonNull(taskType, "taskType must not be null");
		title = requireText(title, "title");
		description = description == null || description.isBlank() ? null : description.strip();
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.strip();
	}
}
