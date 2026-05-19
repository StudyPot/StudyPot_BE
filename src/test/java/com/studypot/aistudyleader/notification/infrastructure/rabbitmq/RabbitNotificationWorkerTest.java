package com.studypot.aistudyleader.notification.infrastructure.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.studypot.aistudyleader.notification.domain.Notification;
import com.studypot.aistudyleader.notification.domain.NotificationAccessContext;
import com.studypot.aistudyleader.notification.domain.NotificationChannel;
import com.studypot.aistudyleader.notification.domain.NotificationRelatedResources;
import com.studypot.aistudyleader.notification.domain.NotificationStatus;
import com.studypot.aistudyleader.notification.domain.NotificationType;
import com.studypot.aistudyleader.notification.repository.NotificationRepository;
import com.studypot.aistudyleader.notification.service.CreateNotificationCommand;
import com.studypot.aistudyleader.notification.service.NotificationService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.core.RabbitOperations;

class RabbitNotificationWorkerTest {

	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000008421");
	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000008422");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000008423");
	private static final UUID FIRST_NOTIFICATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000008424");
	private static final UUID SECOND_NOTIFICATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000008425");
	private static final Instant NOW = Instant.parse("2026-05-19T11:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Test
	void publisherSendsNotificationCommandToConfiguredExchangeAndRoutingKey() {
		RabbitOperations rabbit = mock(RabbitOperations.class);
		NotificationRabbitProperties properties = new NotificationRabbitProperties(
			true,
			"studypot.notification.events",
			"studypot.notification.events",
			"notification.create"
		);
		RabbitNotificationJobPublisher publisher = new RabbitNotificationJobPublisher(rabbit, properties);
		CreateNotificationCommand command = command("notification:rabbit:publish");

		publisher.publish(command);

		verify(rabbit).convertAndSend("studypot.notification.events", "notification.create", command);
	}

	@Test
	void workerCreatesNotificationAndIgnoresDuplicateIdempotencyKey() {
		FakeRepository repository = new FakeRepository();
		NotificationService service = new NotificationService(repository, CLOCK, repository::nextId);
		RabbitNotificationJobWorker worker = new RabbitNotificationJobWorker(service);
		CreateNotificationCommand command = command("notification:rabbit:dedupe");

		worker.handle(command);
		worker.handle(command);

		assertThat(repository.savedNotifications).hasSize(1);
		Notification notification = repository.savedNotifications.getFirst();
		assertThat(notification.status()).isEqualTo(NotificationStatus.DELIVERED);
		assertThat(notification.channel()).isEqualTo(NotificationChannel.IN_APP);
		assertThat(notification.idempotencyKey()).isEqualTo("notification:rabbit:dedupe");
	}

	@Test
	void workerRecordsRedactedFailureAndRejectsWithoutRequeue() {
		FakeRepository repository = new FakeRepository();
		repository.throwOnSave = true;
		NotificationService service = new NotificationService(repository, CLOCK, repository::nextId);
		RabbitNotificationJobWorker worker = new RabbitNotificationJobWorker(service);

		assertThatThrownBy(() -> worker.handle(command("notification:rabbit:failure")))
			.isInstanceOf(AmqpRejectAndDontRequeueException.class)
			.hasMessageContaining("notification job failed");

		assertThat(repository.failedNotification).isNotNull();
		assertThat(repository.failedNotification.status()).isEqualTo(NotificationStatus.FAILED);
		assertThat(repository.failedNotification.retryCount()).isEqualTo(1);
		assertThat(repository.failedNotification.errorMessage())
			.contains("token=[REDACTED]")
			.doesNotContain("plain", "sk-live-secret");
	}

	@Test
	void propertiesRejectBlankRoutingValues() {
		assertThatThrownBy(() -> new NotificationRabbitProperties(true, " ", "queue", "notification.create"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("exchange must not be blank.");
		assertThatThrownBy(() -> new NotificationRabbitProperties(true, "exchange", "", "notification.create"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("queue must not be blank.");
		assertThatThrownBy(() -> new NotificationRabbitProperties(true, "exchange", "queue", "\t"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("routingKey must not be blank.");
	}

	private static CreateNotificationCommand command(String idempotencyKey) {
		return new CreateNotificationCommand(
			GROUP_ID,
			USER_ID,
			new NotificationRelatedResources(null, WEEK_ID, null, null),
			NotificationType.WEEK_STARTED,
			idempotencyKey,
			"알림 제목",
			"알림 본문",
			Map.of("deepLink", "/weeks/%s/tasks".formatted(WEEK_ID)),
			null
		);
	}

	private static final class FakeRepository implements NotificationRepository {

		private final List<Notification> savedNotifications = new ArrayList<>();
		private Notification failedNotification;
		private boolean throwOnSave;
		private int nextIdIndex;

		private UUID nextId() {
			UUID id = nextIdIndex == 0 ? FIRST_NOTIFICATION_ID : SECOND_NOTIFICATION_ID;
			nextIdIndex++;
			return id;
		}

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			return true;
		}

		@Override
		public Optional<NotificationAccessContext> findAccessContext(UUID groupId, UUID userId) {
			return Optional.empty();
		}

		@Override
		public Optional<Notification> findNotification(UUID notificationId) {
			return savedNotifications.stream()
				.filter(notification -> notification.id().equals(notificationId))
				.findFirst();
		}

		@Override
		public Optional<Notification> findNotificationByIdempotencyKey(String idempotencyKey) {
			return savedNotifications.stream()
				.filter(notification -> notification.idempotencyKey().equals(idempotencyKey))
				.findFirst();
		}

		@Override
		public List<Notification> findMyNotifications(UUID userId, boolean unreadOnly, int limit) {
			return List.of();
		}

		@Override
		public List<Notification> findGroupNotifications(UUID groupId, int limit) {
			return List.of();
		}

		@Override
		public List<UUID> findActiveGroupRecipientUserIds(UUID groupId) {
			return List.of();
		}

		@Override
		public Notification saveNotification(Notification notification) {
			if (throwOnSave) {
				throw new IllegalStateException("token=plain sk-live-secret");
			}
			Optional<Notification> existing = findNotificationByIdempotencyKey(notification.idempotencyKey());
			if (existing.isPresent()) {
				return existing.orElseThrow();
			}
			savedNotifications.add(notification);
			return notification;
		}

		@Override
		public Notification recordFailedNotification(Notification notification) {
			failedNotification = notification;
			return notification;
		}

		@Override
		public Notification retryFailedNotification(UUID notificationId, Instant deliveredAt) {
			throw new UnsupportedOperationException("not needed in this test");
		}

		@Override
		public boolean markNotificationRead(UUID notificationId, UUID recipientUserId, Instant readAt) {
			return false;
		}

		@Override
		public int markAllDeliveredNotificationsRead(UUID recipientUserId, Instant readAt) {
			return 0;
		}
	}
}
