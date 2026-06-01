package com.studypot.aistudyleader.notification.controller;

import com.studypot.aistudyleader.notification.domain.Notification;
import com.studypot.aistudyleader.notification.service.NotificationStreamPublisher;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
class NotificationStreamService implements NotificationStreamPublisher {

	static final String CONNECTED_EVENT_NAME = "connected";
	static final String NOTIFICATION_CREATED_EVENT_NAME = "notification-created";
	private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(30);
	private static final Logger log = LoggerFactory.getLogger(NotificationStreamService.class);

	private final ConcurrentMap<UUID, Set<NotificationStreamConnection>> connectionsByRecipient = new ConcurrentHashMap<>();

	SseEmitter subscribe(UUID recipientUserId) {
		Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
		SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT.toMillis());
		NotificationStreamConnection connection = new SseNotificationStreamConnection(emitter);
		register(recipientUserId, connection);
		sendConnectedEvent(recipientUserId, connection);
		return emitter;
	}

	void register(UUID recipientUserId, NotificationStreamConnection connection) {
		Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
		Objects.requireNonNull(connection, "connection must not be null");
		Runnable cleanup = () -> remove(recipientUserId, connection);
		connection.onCompletion(cleanup);
		connection.onTimeout(cleanup);
		connection.onError(throwable -> cleanup.run());
		connectionsByRecipient.compute(recipientUserId, (ignored, connections) -> {
			Set<NotificationStreamConnection> activeConnections = connections == null
				? ConcurrentHashMap.newKeySet()
				: connections;
			activeConnections.add(connection);
			return activeConnections;
		});
	}

	int activeConnectionCount(UUID recipientUserId) {
		Set<NotificationStreamConnection> connections = connectionsByRecipient.get(recipientUserId);
		return connections == null ? 0 : connections.size();
	}

	@Override
	public void publishNotificationCreated(Notification notification) {
		Objects.requireNonNull(notification, "notification must not be null");
		publish(notification.recipientUserId(), NOTIFICATION_CREATED_EVENT_NAME, NotificationResponse.from(notification));
	}

	private void sendConnectedEvent(UUID recipientUserId, NotificationStreamConnection connection) {
		try {
			connection.send(CONNECTED_EVENT_NAME, Map.of("stream", "notifications"));
		} catch (IOException | RuntimeException exception) {
			remove(recipientUserId, connection);
			connection.completeWithError(exception);
		}
	}

	private void publish(UUID recipientUserId, String eventName, Object data) {
		Set<NotificationStreamConnection> connections = connectionsByRecipient.get(recipientUserId);
		if (connections == null || connections.isEmpty()) {
			return;
		}
		for (NotificationStreamConnection connection : List.copyOf(connections)) {
			try {
				connection.send(eventName, data);
			} catch (IOException | RuntimeException exception) {
				remove(recipientUserId, connection);
				connection.completeWithError(exception);
				log.debug("notification SSE send failed recipientUserId={} eventName={}", recipientUserId, eventName, exception);
			}
		}
	}

	private void remove(UUID recipientUserId, NotificationStreamConnection connection) {
		connectionsByRecipient.computeIfPresent(recipientUserId, (ignored, connections) -> {
			connections.remove(connection);
			return connections.isEmpty() ? null : connections;
		});
	}

	private static final class SseNotificationStreamConnection implements NotificationStreamConnection {

		private static final long RECONNECT_TIME_MILLIS = Duration.ofSeconds(5).toMillis();

		private final SseEmitter emitter;

		private SseNotificationStreamConnection(SseEmitter emitter) {
			this.emitter = Objects.requireNonNull(emitter, "emitter must not be null");
		}

		@Override
		public void send(String eventName, Object data) throws IOException {
			emitter.send(SseEmitter.event()
				.name(eventName)
				.reconnectTime(RECONNECT_TIME_MILLIS)
				.data(data));
		}

		@Override
		public void onCompletion(Runnable callback) {
			emitter.onCompletion(callback);
		}

		@Override
		public void onTimeout(Runnable callback) {
			emitter.onTimeout(callback);
		}

		@Override
		public void onError(Consumer<Throwable> callback) {
			emitter.onError(callback);
		}

		@Override
		public void completeWithError(Throwable throwable) {
			emitter.completeWithError(throwable);
		}
	}
}

interface NotificationStreamConnection {

	void send(String eventName, Object data) throws IOException;

	void onCompletion(Runnable callback);

	void onTimeout(Runnable callback);

	void onError(Consumer<Throwable> callback);

	void completeWithError(Throwable throwable);
}
