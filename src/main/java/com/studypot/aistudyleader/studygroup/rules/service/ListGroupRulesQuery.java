package com.studypot.aistudyleader.studygroup.rules.service;

import java.util.Objects;
import java.util.UUID;

public record ListGroupRulesQuery(UUID authenticatedUserId, UUID groupId) {

	public ListGroupRulesQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
	}
}
