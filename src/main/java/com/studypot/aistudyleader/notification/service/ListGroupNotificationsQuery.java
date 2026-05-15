package com.studypot.aistudyleader.notification.service;

import java.util.Objects;
import java.util.UUID;

public record ListGroupNotificationsQuery(UUID authenticatedUserId, UUID groupId) {

	public ListGroupNotificationsQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
	}
}
