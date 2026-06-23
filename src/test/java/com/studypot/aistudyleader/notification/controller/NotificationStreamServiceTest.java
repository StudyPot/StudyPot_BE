package com.studypot.aistudyleader.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.studypot.aistudyleader.notification.domain.Notification;
import com.studypot.aistudyleader.notification.domain.NotificationChannel;
import com.studypot.aistudyleader.notification.domain.NotificationRelatedResources;
import com.studypot.aistudyleader.notification.domain.NotificationStatus;
import com.studypot.aistudyleader.notification.domain.NotificationType;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class NotificationStreamServiceTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000008401");
	private static final UUID OTHER_USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000008402");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000008403");
	private static final UUID OTHER_GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000008404");
	private static final UUID NOTIFICATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000008405");
	private static final Instant NOW = Instant.parse("2026-06-01T01:00:00Z");

	@Test
	void publishNotificationCreatedSendsOnlyToRecipientConnections() {
		NotificationStreamService service = new NotificationStreamService();
		RecordingConnection recipientConnection = new RecordingConnection();
		RecordingConnection otherConnection = new RecordingConnection();
		service.register(USER_ID, recipientConnection);
		service.register(OTHER_USER_ID, otherConnection);
		recipientConnection.clear();
		otherConnection.clear();

		service.publishNotificationCreated(notification(GROUP_ID, USER_ID));

		assertThat(recipientConnection.events)
			.singleElement()
			.satisfies(event -> {
				assertThat(event.name()).isEqualTo("notification-created");
				assertThat(event.data()).isInstanceOf(NotificationResponse.class);
				assertThat(((NotificationResponse) event.data()).id()).isEqualTo(NOTIFICATION_ID);
			});
		assertThat(otherConnection.events).isEmpty();
	}

	@Test
	void publishNotificationCreatedUsesRecipientEvenWhenNotificationComesFromAnotherGroup() {
		NotificationStreamService service = new NotificationStreamService();
		RecordingConnection recipientConnection = new RecordingConnection();
		service.register(USER_ID, recipientConnection);
		recipientConnection.clear();

		service.publishNotificationCreated(notification(OTHER_GROUP_ID, USER_ID));

		assertThat(recipientConnection.events)
			.extracting(RecordedEvent::name)
			.containsExactly("notification-created");
	}

	@Test
	void failingConnectionIsRemovedWithoutStoppingOtherConnections() {
		NotificationStreamService service = new NotificationStreamService();
		RecordingConnection healthyConnection = new RecordingConnection();
		FailingConnection failingConnection = new FailingConnection();
		service.register(USER_ID, healthyConnection);
		service.register(USER_ID, failingConnection);
		healthyConnection.clear();

		service.publishNotificationCreated(notification(GROUP_ID, USER_ID));

		assertThat(healthyConnection.events)
			.extracting(RecordedEvent::name)
			.containsExactly("notification-created");
		assertThat(failingConnection.completedWithError).isTrue();
		assertThat(service.activeConnectionCount(USER_ID)).isEqualTo(1);
	}

	@Test
	void heartbeatPingsAllActiveConnectionsAndRemovesDeadOnes() {
		NotificationStreamService service = new NotificationStreamService();
		RecordingConnection healthyConnection = new RecordingConnection();
		FailingConnection deadConnection = new FailingConnection();
		service.register(USER_ID, healthyConnection);
		service.register(OTHER_USER_ID, deadConnection);
		healthyConnection.clear();

		service.sendHeartbeats();

		assertThat(healthyConnection.heartbeats).isEqualTo(1);
		assertThat(deadConnection.completedWithError).isTrue();
		assertThat(service.activeConnectionCount(USER_ID)).isEqualTo(1);
		assertThat(service.activeConnectionCount(OTHER_USER_ID)).isZero();
	}

	@Test
	void timeoutCompletesEmitterBeforeRemovingConnection() {
		NotificationStreamService service = new NotificationStreamService();
		RecordingConnection timedOut = new RecordingConnection();
		service.register(USER_ID, timedOut);

		timedOut.timeout.run();

		assertThat(timedOut.completed).isTrue();
		assertThat(service.activeConnectionCount(USER_ID)).isZero();
	}

	@Test
	void completionTimeoutAndErrorCallbacksRemoveConnection() {
		NotificationStreamService service = new NotificationStreamService();
		RecordingConnection completed = new RecordingConnection();
		RecordingConnection timedOut = new RecordingConnection();
		RecordingConnection errored = new RecordingConnection();
		service.register(USER_ID, completed);
		service.register(USER_ID, timedOut);
		service.register(USER_ID, errored);

		completed.completion.run();
		timedOut.timeout.run();
		errored.error.accept(new IOException("client disconnected"));

		assertThat(service.activeConnectionCount(USER_ID)).isZero();
	}

	private static Notification notification(UUID groupId, UUID recipientUserId) {
		return new Notification(
			NOTIFICATION_ID,
			groupId,
			recipientUserId,
			new NotificationRelatedResources(null, null, null, null),
			NotificationType.RETROSPECTIVE_READY,
			NotificationChannel.IN_APP,
			"notification:test",
			"회고 피드백이 준비됐어요",
			"AI 팀장 피드백을 확인해 주세요.",
			Map.of("deepLink", "/retrospectives"),
			NotificationStatus.DELIVERED,
			NOW,
			null,
			null,
			0,
			null,
			NOW
		);
	}

	private record RecordedEvent(String name, Object data) {
	}

	private static class RecordingConnection implements NotificationStreamConnection {

		private final List<RecordedEvent> events = new ArrayList<>();
		private int heartbeats;
		private boolean completed;
		private Runnable completion = () -> {
		};
		private Runnable timeout = () -> {
		};
		private Consumer<Throwable> error = ignored -> {
		};

		@Override
		public void send(String eventName, Object data) throws IOException {
			events.add(new RecordedEvent(eventName, data));
		}

		@Override
		public void sendHeartbeat() throws IOException {
			heartbeats++;
		}

		@Override
		public void onCompletion(Runnable callback) {
			completion = callback;
		}

		@Override
		public void onTimeout(Runnable callback) {
			timeout = callback;
		}

		@Override
		public void onError(Consumer<Throwable> callback) {
			error = callback;
		}

		@Override
		public void complete() {
			completed = true;
		}

		@Override
		public void completeWithError(Throwable throwable) {
		}

		void clear() {
			events.clear();
		}
	}

	private static final class FailingConnection extends RecordingConnection {

		private boolean completedWithError;

		@Override
		public void send(String eventName, Object data) throws IOException {
			throw new IOException("client disconnected");
		}

		@Override
		public void sendHeartbeat() throws IOException {
			throw new IOException("client disconnected");
		}

		@Override
		public void completeWithError(Throwable throwable) {
			completedWithError = true;
		}
	}
}
