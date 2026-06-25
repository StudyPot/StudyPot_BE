package com.studypot.aistudyleader.studygroup.service;

import java.util.Objects;
import java.util.UUID;

public record GetAiManagerQuery(
	UUID authenticatedUserId,
	UUID groupId
) {

	public GetAiManagerQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
	}
}
