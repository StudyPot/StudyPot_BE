package com.studypot.aistudyleader.notification.service;

import java.util.Objects;
import java.util.UUID;

public record ListMyNotificationsQuery(UUID authenticatedUserId, boolean unreadOnly, String cursor) {

	public ListMyNotificationsQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		cursor = cursor == null || cursor.isBlank() ? null : cursor.strip();
	}
}
