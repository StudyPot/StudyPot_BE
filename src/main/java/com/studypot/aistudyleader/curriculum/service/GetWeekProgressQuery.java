package com.studypot.aistudyleader.curriculum.service;

import java.util.Objects;
import java.util.UUID;

public record GetWeekProgressQuery(
	UUID authenticatedUserId,
	UUID weekId
) {

	public GetWeekProgressQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(weekId, "weekId must not be null");
	}
}
