package com.studypot.aistudyleader.notification.service;

import java.util.Objects;
import java.util.UUID;

public record MarkAllMyNotificationsReadCommand(UUID authenticatedUserId) {

	public MarkAllMyNotificationsReadCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
	}
}
