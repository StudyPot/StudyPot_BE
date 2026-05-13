package com.studypot.aistudyleader.notification.service;

import java.util.Objects;
import java.util.UUID;

public record MarkNotificationReadCommand(UUID authenticatedUserId, UUID notificationId) {

	public MarkNotificationReadCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(notificationId, "notificationId must not be null");
	}
}
