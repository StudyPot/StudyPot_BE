package com.studypot.aistudyleader.retrospective.service;

import java.util.Objects;
import java.util.UUID;

public record GetMyRetrospectiveQuery(
	UUID authenticatedUserId,
	UUID weekId
) {

	public GetMyRetrospectiveQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(weekId, "weekId must not be null");
	}
}
