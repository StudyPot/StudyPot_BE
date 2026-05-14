package com.studypot.aistudyleader.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationTest {

	private static final UUID NOTIFICATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000008001");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000008002");
	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000008003");
	private static final Instant NOW = Instant.parse("2026-05-13T05:00:00Z");

	@Test
	void markReadTurnsDeliveredNotificationIntoReadState() {
		Notification delivered = notification(NotificationStatus.DELIVERED, null);

		Notification read = delivered.markRead(NOW.plusSeconds(60));

		assertThat(read.status()).isEqualTo(NotificationStatus.READ);
		assertThat(read.readAt()).isEqualTo(NOW.plusSeconds(60));
		assertThat(read.deliveredAt()).isEqualTo(NOW);
	}

	@Test
	void markReadIsIdempotentForAlreadyReadNotification() {
		Notification read = notification(NotificationStatus.READ, NOW.plusSeconds(30));

		assertThat(read.markRead(NOW.plusSeconds(60))).isSameAs(read);
	}

	@Test
	void markReadRejectsPendingNotification() {
		Notification pending = notification(NotificationStatus.PENDING, null);

		assertThatThrownBy(() -> pending.markRead(NOW.plusSeconds(60)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("only delivered notifications can be marked read.");
	}

	@Test
	void readStatusRequiresReadAt() {
		assertThatThrownBy(() -> notification(NotificationStatus.READ, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("readAt is required when notification status is READ.");
	}

	@Test
	void readStatusRequiresDeliveredAt() {
		assertThatThrownBy(() -> new Notification(
			NOTIFICATION_ID,
			GROUP_ID,
			USER_ID,
			new NotificationRelatedResources(null, null, null, null),
			NotificationType.RETROSPECTIVE_READY,
			NotificationChannel.IN_APP,
			"notification:test",
			"회고 피드백이 준비됐어요",
			"AI 팀장 피드백을 확인해 주세요.",
			Map.of("deepLink", "/retrospectives"),
			NotificationStatus.READ,
			null,
			NOW.plusSeconds(30),
			null,
			0,
			null,
			NOW.minusSeconds(30)
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("deliveredAt is required when notification status is READ.");
	}

	@Test
	void deliveredStatusRequiresDeliveredAt() {
		assertThatThrownBy(() -> new Notification(
			NOTIFICATION_ID,
			GROUP_ID,
			USER_ID,
			new NotificationRelatedResources(null, null, null, null),
			NotificationType.RETROSPECTIVE_READY,
			NotificationChannel.IN_APP,
			"notification:test",
			"회고 피드백이 준비됐어요",
			"AI 팀장 피드백을 확인해 주세요.",
			Map.of("deepLink", "/retrospectives"),
			NotificationStatus.DELIVERED,
			null,
			null,
			null,
			0,
			null,
			NOW.minusSeconds(30)
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("deliveredAt is required when notification status is DELIVERED.");
	}

	@Test
	void recordFailureMarksPendingOrDeliveredNotificationFailed() {
		Notification delivered = notification(NotificationStatus.DELIVERED, null);

		Notification failed = delivered.recordFailure("provider timeout", NOW.plusSeconds(60));

		assertThat(failed.status()).isEqualTo(NotificationStatus.FAILED);
		assertThat(failed.deliveredAt()).isNull();
		assertThat(failed.readAt()).isNull();
		assertThat(failed.errorMessage()).isEqualTo("provider timeout");
		assertThat(failed.retryCount()).isEqualTo(1);
	}

	@Test
	void retryDeliveredRestoresFailedNotificationToDeliveredState() {
		Notification failed = notification(NotificationStatus.PENDING, null)
			.recordFailure("provider timeout", NOW.plusSeconds(60));

		Notification delivered = failed.retryDelivered(NOW.plusSeconds(120));

		assertThat(delivered.status()).isEqualTo(NotificationStatus.DELIVERED);
		assertThat(delivered.deliveredAt()).isEqualTo(NOW.plusSeconds(120));
		assertThat(delivered.errorMessage()).isNull();
		assertThat(delivered.retryCount()).isEqualTo(1);
	}

	@Test
	void payloadRejectsNullKeysOrValues() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("deepLink", null);

		assertThatThrownBy(() -> new Notification(
			NOTIFICATION_ID,
			GROUP_ID,
			USER_ID,
			new NotificationRelatedResources(null, null, null, null),
			NotificationType.RETROSPECTIVE_READY,
			NotificationChannel.IN_APP,
			"notification:test",
			"회고 피드백이 준비됐어요",
			"AI 팀장 피드백을 확인해 주세요.",
			payload,
			NotificationStatus.DELIVERED,
			NOW,
			null,
			null,
			0,
			null,
			NOW.minusSeconds(30)
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("payload must not contain null keys or values.");
	}

	private static Notification notification(NotificationStatus status, Instant readAt) {
		return new Notification(
			NOTIFICATION_ID,
			GROUP_ID,
			USER_ID,
			new NotificationRelatedResources(null, null, null, null),
			NotificationType.RETROSPECTIVE_READY,
			NotificationChannel.IN_APP,
			"notification:test",
			"회고 피드백이 준비됐어요",
			"AI 팀장 피드백을 확인해 주세요.",
			Map.of("deepLink", "/retrospectives"),
			status,
			status == NotificationStatus.PENDING ? null : NOW,
			readAt,
			null,
			0,
			null,
			NOW.minusSeconds(30)
		);
	}
}
