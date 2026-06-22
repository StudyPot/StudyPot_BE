package com.studypot.aistudyleader.onboarding.service;

import java.util.Objects;
import java.util.UUID;

public record GetGroupOnboardingsQuery(
	UUID authenticatedUserId,
	UUID groupId
) {

	public GetGroupOnboardingsQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
	}
}
