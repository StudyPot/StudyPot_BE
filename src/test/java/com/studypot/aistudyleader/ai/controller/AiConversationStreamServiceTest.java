package com.studypot.aistudyleader.ai.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageSenderType;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class AiConversationStreamServiceTest {

	private static final UUID CONVERSATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000009401");
	private static final UUID OTHER_CONVERSATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000009402");
	private static final UUID USER_MESSAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000009403");
	private static final UUID ASSISTANT_MESSAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000009404");
	private static final Instant NOW = Instant.parse("2026-06-01T02:00:00Z");

	@Test
	void publishUserMessageSavedSendsOnlyToConversationConnections() {
		AiConversationStreamService service = new AiConversationStreamService();
		RecordingConnection conversationConnection = new RecordingConnection();
		RecordingConnection otherConnection = new RecordingConnection();
		service.register(CONVERSATION_ID, conversationConnection);
		service.register(OTHER_CONVERSATION_ID, otherConnection);
		conversationConnection.clear();
		otherConnection.clear();

		service.publishUserMessageSaved(userMessage());

		assertThat(conversationConnection.events)
			.singleElement()
			.satisfies(event -> {
				assertThat(event.name()).isEqualTo("user-message-saved");
				assertThat(event.data()).isInstanceOf(AiConversationMessageResponse.class);
				assertThat(((AiConversationMessageResponse) event.data()).id()).isEqualTo(USER_MESSAGE_ID);
			});
		assertThat(otherConnection.events).isEmpty();
	}

	@Test
	void publishAssistantLifecycleEventsUseConversationIdAndMessageConversation() {
		AiConversationStreamService service = new AiConversationStreamService();
		RecordingConnection connection = new RecordingConnection();
		service.register(CONVERSATION_ID, connection);
		connection.clear();

		service.publishAssistantGenerationStarted(CONVERSATION_ID);
		service.publishAssistantMessageCreated(assistantMessage());
		service.publishAssistantGenerationFailed(CONVERSATION_ID, "AI_CHAT_TIMEOUT");

		assertThat(connection.events)
			.extracting(RecordedEvent::name)
			.containsExactly("assistant-generation-started", "assistant-message-created", "assistant-generation-failed");
		assertThat(connection.events.get(1).data()).isInstanceOf(AiConversationMessageResponse.class);
		assertThat(connection.events.get(2).data()).isInstanceOf(AiConversationGenerationFailedResponse.class);
	}

	@Test
	void failingConnectionIsRemovedWithoutStoppingOtherConnections() {
		AiConversationStreamService service = new AiConversationStreamService();
		RecordingConnection healthyConnection = new RecordingConnection();
		FailingConnection failingConnection = new FailingConnection();
		service.register(CONVERSATION_ID, healthyConnection);
		service.register(CONVERSATION_ID, failingConnection);
		healthyConnection.clear();

		service.publishUserMessageSaved(userMessage());

		assertThat(healthyConnection.events)
			.extracting(RecordedEvent::name)
			.containsExactly("user-message-saved");
		assertThat(failingConnection.completedWithError).isTrue();
		assertThat(service.activeConnectionCount(CONVERSATION_ID)).isEqualTo(1);
	}

	@Test
	void completionTimeoutAndErrorCallbacksRemoveConnection() {
		AiConversationStreamService service = new AiConversationStreamService();
		RecordingConnection completed = new RecordingConnection();
		RecordingConnection timedOut = new RecordingConnection();
		RecordingConnection errored = new RecordingConnection();
		service.register(CONVERSATION_ID, completed);
		service.register(CONVERSATION_ID, timedOut);
		service.register(CONVERSATION_ID, errored);

		completed.completion.run();
		timedOut.timeout.run();
		errored.error.accept(new IOException("client disconnected"));

		assertThat(service.activeConnectionCount(CONVERSATION_ID)).isZero();
	}

	private static AiConversationMessage userMessage() {
		return AiConversationMessage.userMessage(USER_MESSAGE_ID, CONVERSATION_ID, "사용자 메시지", NOW);
	}

	private static AiConversationMessage assistantMessage() {
		return new AiConversationMessage(
			ASSISTANT_MESSAGE_ID,
			CONVERSATION_ID,
			null,
			AiConversationMessageSenderType.ASSISTANT,
			"AI 응답",
			Map.of(),
			NOW
		);
	}

	private record RecordedEvent(String name, Object data) {
	}

	private static class RecordingConnection implements AiConversationStreamConnection {

		private final List<RecordedEvent> events = new ArrayList<>();
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
		public void completeWithError(Throwable throwable) {
			completedWithError = true;
		}
	}
}
