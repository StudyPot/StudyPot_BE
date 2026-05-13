package com.studypot.aistudyleader.llm.service;

import java.util.Objects;
import java.util.UUID;

public record ListGroupLlmUsageQuery(UUID authenticatedUserId, UUID groupId) {

	public ListGroupLlmUsageQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
	}
}
