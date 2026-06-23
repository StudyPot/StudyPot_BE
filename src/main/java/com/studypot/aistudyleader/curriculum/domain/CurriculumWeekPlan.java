package com.studypot.aistudyleader.curriculum.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record CurriculumWeekPlan(
	int weekNumber,
	String title,
	String sprintGoal,
	List<RetrospectiveQuestion> retrospectiveQuestions,
	List<String> learningGoals,
	List<Map<String, String>> resources,
	List<CurriculumTaskPlan> tasks
) {

	public CurriculumWeekPlan {
		if (weekNumber <= 0) {
			throw new IllegalArgumentException("weekNumber must be positive");
		}
		title = requireText(title, "title");
		sprintGoal = sprintGoal == null || sprintGoal.isBlank() ? null : sprintGoal.strip();
		retrospectiveQuestions = List.copyOf(Objects.requireNonNull(retrospectiveQuestions, "retrospectiveQuestions must not be null"));
		learningGoals = List.copyOf(Objects.requireNonNull(learningGoals, "learningGoals must not be null"));
		resources = List.copyOf(Objects.requireNonNull(resources, "resources must not be null"));
		tasks = List.copyOf(Objects.requireNonNull(tasks, "tasks must not be null"));
		if (tasks.isEmpty()) {
			throw new IllegalArgumentException("tasks must not be empty");
		}
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.strip();
	}
}
