package com.studypot.aistudyleader.studygroup.service;

import java.util.Objects;
import java.util.UUID;

public record ListStudyGroupsQuery(UUID authenticatedUserId) {

	public ListStudyGroupsQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
	}
}
