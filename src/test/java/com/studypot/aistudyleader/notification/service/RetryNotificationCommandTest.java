package com.studypot.aistudyleader.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RetryNotificationCommandTest {

	private static final UUID NOTIFICATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000008901");

	@Test
	void createsCommandWithNotificationId() {
		RetryNotificationCommand command = new RetryNotificationCommand(NOTIFICATION_ID);

		assertThat(command.notificationId()).isEqualTo(NOTIFICATION_ID);
	}

	@Test
	void rejectsNullNotificationId() {
		assertThatThrownBy(() -> new RetryNotificationCommand(null))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("notificationId must not be null");
	}
}
