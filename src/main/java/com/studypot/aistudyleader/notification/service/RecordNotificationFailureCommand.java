package com.studypot.aistudyleader.notification.service;

import java.util.Objects;

public record RecordNotificationFailureCommand(
	CreateNotificationCommand notification,
	String errorMessage
) {

	public RecordNotificationFailureCommand {
		Objects.requireNonNull(notification, "notification must not be null");
		errorMessage = errorMessage == null || errorMessage.isBlank() ? "notification delivery failed." : errorMessage.strip();
	}
}
