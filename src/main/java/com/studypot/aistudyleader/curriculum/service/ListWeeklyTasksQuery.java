package com.studypot.aistudyleader.curriculum.service;

import java.util.Objects;
import java.util.UUID;

public record ListWeeklyTasksQuery(
	UUID authenticatedUserId,
	UUID weekId
) {

	public ListWeeklyTasksQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(weekId, "weekId must not be null");
	}
}
