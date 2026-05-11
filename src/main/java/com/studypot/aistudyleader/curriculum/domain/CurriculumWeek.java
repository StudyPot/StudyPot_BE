package com.studypot.aistudyleader.curriculum.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record CurriculumWeek(
	UUID id,
	UUID curriculumId,
	int weekNumber,
	String title,
	String description,
	String sprintGoal,
	List<String> learningGoals,
	List<Map<String, String>> resources,
	CurriculumWeekStatus status,
	Instant startsAt,
	Instant endsAt,
	List<WeeklyTask> tasks,
	Instant createdAt,
	Instant updatedAt
) {

	public CurriculumWeek {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(curriculumId, "curriculumId must not be null");
		if (weekNumber <= 0) {
			throw new IllegalArgumentException("weekNumber must be positive");
		}
		title = requireText(title, "title");
		description = description == null || description.isBlank() ? null : description.strip();
		sprintGoal = sprintGoal == null || sprintGoal.isBlank() ? null : sprintGoal.strip();
		learningGoals = List.copyOf(Objects.requireNonNull(learningGoals, "learningGoals must not be null"));
		resources = List.copyOf(Objects.requireNonNull(resources, "resources must not be null"));
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(startsAt, "startsAt must not be null");
		Objects.requireNonNull(endsAt, "endsAt must not be null");
		if (!endsAt.isAfter(startsAt)) {
			throw new IllegalArgumentException("endsAt must be after startsAt");
		}
		tasks = List.copyOf(Objects.requireNonNull(tasks, "tasks must not be null"));
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
