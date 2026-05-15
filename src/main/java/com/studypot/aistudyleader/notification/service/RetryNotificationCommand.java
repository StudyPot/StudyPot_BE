package com.studypot.aistudyleader.notification.service;

import java.util.Objects;
import java.util.UUID;

public record RetryNotificationCommand(UUID notificationId) {

	public RetryNotificationCommand {
		Objects.requireNonNull(notificationId, "notificationId must not be null");
	}
}
