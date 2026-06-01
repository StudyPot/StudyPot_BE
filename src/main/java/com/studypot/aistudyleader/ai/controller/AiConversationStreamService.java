package com.studypot.aistudyleader.ai.controller;

import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import com.studypot.aistudyleader.ai.service.AiConversationStreamPublisher;
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
class AiConversationStreamService implements AiConversationStreamPublisher {

	static final String CONNECTED_EVENT_NAME = "connected";
	private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(30);
	private static final Logger log = LoggerFactory.getLogger(AiConversationStreamService.class);

	private final ConcurrentMap<UUID, Set<AiConversationStreamConnection>> connectionsByConversation = new ConcurrentHashMap<>();

	SseEmitter subscribe(UUID conversationId) {
		Objects.requireNonNull(conversationId, "conversationId must not be null");
		SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT.toMillis());
		AiConversationStreamConnection connection = new SseAiConversationStreamConnection(emitter);
		register(conversationId, connection);
		sendConnectedEvent(conversationId, connection);
		return emitter;
	}

	void register(UUID conversationId, AiConversationStreamConnection connection) {
		Objects.requireNonNull(conversationId, "conversationId must not be null");
		Objects.requireNonNull(connection, "connection must not be null");
		Runnable cleanup = () -> remove(conversationId, connection);
		connection.onCompletion(cleanup);
		connection.onTimeout(cleanup);
		connection.onError(throwable -> cleanup.run());
		connectionsByConversation.compute(conversationId, (ignored, connections) -> {
			Set<AiConversationStreamConnection> activeConnections = connections == null
				? ConcurrentHashMap.newKeySet()
				: connections;
			activeConnections.add(connection);
			return activeConnections;
		});
	}

	int activeConnectionCount(UUID conversationId) {
		Set<AiConversationStreamConnection> connections = connectionsByConversation.get(conversationId);
		return connections == null ? 0 : connections.size();
	}

	@Override
	public void publishUserMessageSaved(AiConversationMessage message) {
		Objects.requireNonNull(message, "message must not be null");
		publish(message.conversationId(), USER_MESSAGE_SAVED_EVENT, AiConversationMessageResponse.from(message));
	}

	@Override
	public void publishAssistantGenerationStarted(UUID conversationId) {
		Objects.requireNonNull(conversationId, "conversationId must not be null");
		publish(conversationId, ASSISTANT_GENERATION_STARTED_EVENT,
			Map.of("conversationId", conversationId.toString()));
	}

	@Override
	public void publishAssistantMessageCreated(AiConversationMessage message) {
		Objects.requireNonNull(message, "message must not be null");
		publish(message.conversationId(), ASSISTANT_MESSAGE_CREATED_EVENT, AiConversationMessageResponse.from(message));
	}

	@Override
	public void publishAssistantGenerationFailed(UUID conversationId, String errorCode) {
		Objects.requireNonNull(conversationId, "conversationId must not be null");
		String safeErrorCode = errorCode == null || errorCode.isBlank() ? "AI_CHAT_FAILED" : errorCode.strip();
		publish(conversationId, ASSISTANT_GENERATION_FAILED_EVENT,
			new AiConversationGenerationFailedResponse(conversationId, safeErrorCode));
	}

	private void sendConnectedEvent(UUID conversationId, AiConversationStreamConnection connection) {
		try {
			connection.send(CONNECTED_EVENT_NAME, Map.of("stream", "ai-conversation"));
		} catch (IOException | RuntimeException exception) {
			remove(conversationId, connection);
			connection.completeWithError(exception);
		}
	}

	private void publish(UUID conversationId, String eventName, Object data) {
		Set<AiConversationStreamConnection> connections = connectionsByConversation.get(conversationId);
		if (connections == null || connections.isEmpty()) {
			return;
		}
		for (AiConversationStreamConnection connection : List.copyOf(connections)) {
			try {
				connection.send(eventName, data);
			} catch (IOException | RuntimeException exception) {
				remove(conversationId, connection);
				connection.completeWithError(exception);
				log.debug("AI conversation SSE send failed conversationId={} eventName={}", conversationId, eventName, exception);
			}
		}
	}

	private void remove(UUID conversationId, AiConversationStreamConnection connection) {
		connectionsByConversation.computeIfPresent(conversationId, (ignored, connections) -> {
			connections.remove(connection);
			return connections.isEmpty() ? null : connections;
		});
	}

	private static final class SseAiConversationStreamConnection implements AiConversationStreamConnection {

		private static final long RECONNECT_TIME_MILLIS = Duration.ofSeconds(5).toMillis();

		private final SseEmitter emitter;

		private SseAiConversationStreamConnection(SseEmitter emitter) {
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

interface AiConversationStreamConnection {

	void send(String eventName, Object data) throws IOException;

	void onCompletion(Runnable callback);

	void onTimeout(Runnable callback);

	void onError(Consumer<Throwable> callback);

	void completeWithError(Throwable throwable);
}

record AiConversationGenerationFailedResponse(UUID conversationId, String errorCode) {
}
