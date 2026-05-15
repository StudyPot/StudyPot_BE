package com.studypot.aistudyleader.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.notification.domain.NotificationRelatedResources;
import com.studypot.aistudyleader.notification.domain.NotificationType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecordNotificationFailureCommandTest {

	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000008911");
	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000008912");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000008913");

	@Test
	void createsCommandWithNotificationAndErrorMessage() {
		CreateNotificationCommand notification = notification();

		RecordNotificationFailureCommand command = new RecordNotificationFailureCommand(notification, "provider timeout");

		assertThat(command.notification()).isSameAs(notification);
		assertThat(command.errorMessage()).isEqualTo("provider timeout");
	}

	@Test
	void rejectsNullNotification() {
		assertThatThrownBy(() -> new RecordNotificationFailureCommand(null, "provider timeout"))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("notification must not be null");
	}

	@Test
	void defaultsMissingErrorMessage() {
		assertThat(new RecordNotificationFailureCommand(notification(), null).errorMessage())
			.isEqualTo("notification delivery failed.");
		assertThat(new RecordNotificationFailureCommand(notification(), " ").errorMessage())
			.isEqualTo("notification delivery failed.");
	}

	@Test
	void stripsErrorMessage() {
		assertThat(new RecordNotificationFailureCommand(notification(), "  provider timeout  ").errorMessage())
			.isEqualTo("provider timeout");
	}

	private static CreateNotificationCommand notification() {
		return new CreateNotificationCommand(
			GROUP_ID,
			USER_ID,
			new NotificationRelatedResources(null, WEEK_ID, null, null),
			NotificationType.WEEK_STARTED,
			"notification:week-started",
			"알림 제목",
			"알림 본문",
			Map.of("deepLink", "/weeks/%s/tasks".formatted(WEEK_ID)),
			null
		);
	}
}
