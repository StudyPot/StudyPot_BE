package com.studypot.aistudyleader.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.studypot.aistudyleader.notification.domain.Notification;
import com.studypot.aistudyleader.notification.domain.NotificationRelatedResources;
import com.studypot.aistudyleader.notification.domain.NotificationType;
import com.studypot.aistudyleader.notification.repository.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class NotificationApplicationConfigurationTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-04T01:00:00Z"), ZoneOffset.UTC);
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000009101");
	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000009102");

	@Test
	void notificationServiceUsesAvailableRealtimeStreamPublisherInsteadOfNoop() {
		NotificationRepository repository = mock(NotificationRepository.class);
		CapturingStreamPublisher streamPublisher = new CapturingStreamPublisher();
		given(repository.saveNotification(any(Notification.class))).willAnswer(invocation -> invocation.getArgument(0));

		new ApplicationContextRunner()
			.withUserConfiguration(NotificationApplicationConfiguration.class)
			.withBean(NotificationRepository.class, () -> repository)
			.withBean(Clock.class, () -> CLOCK)
			.withBean(NotificationStreamPublisher.class, () -> streamPublisher)
			.run(context -> {
				NotificationService service = context.getBean(NotificationService.class);

				Notification created = service.createNotification(command());

				assertThat(streamPublisher.publishedNotifications).containsExactly(created);
			});
	}

	private static CreateNotificationCommand command() {
		return new CreateNotificationCommand(
			GROUP_ID,
			USER_ID,
			new NotificationRelatedResources(null, null, null, null),
			NotificationType.ONBOARDING_REQUESTED,
			"notification:configuration-test",
			"온보딩 요청",
			"스터디 온보딩을 작성해 주세요.",
			Map.of(),
			null
		);
	}

	private static final class CapturingStreamPublisher implements NotificationStreamPublisher {

		private final List<Notification> publishedNotifications = new ArrayList<>();

		@Override
		public void publishNotificationCreated(Notification notification) {
			publishedNotifications.add(notification);
		}
	}
}
