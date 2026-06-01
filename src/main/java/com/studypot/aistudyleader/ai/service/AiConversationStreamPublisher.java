package com.studypot.aistudyleader.ai.service;

import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import java.util.UUID;

public interface AiConversationStreamPublisher {

	String USER_MESSAGE_SAVED_EVENT = "user-message-saved";
	String ASSISTANT_GENERATION_STARTED_EVENT = "assistant-generation-started";
	String ASSISTANT_MESSAGE_CREATED_EVENT = "assistant-message-created";
	String ASSISTANT_GENERATION_FAILED_EVENT = "assistant-generation-failed";

	void publishUserMessageSaved(AiConversationMessage message);

	void publishAssistantGenerationStarted(UUID conversationId);

	void publishAssistantMessageCreated(AiConversationMessage message);

	void publishAssistantGenerationFailed(UUID conversationId, String errorCode);

	static AiConversationStreamPublisher noop() {
		return new AiConversationStreamPublisher() {
			@Override
			public void publishUserMessageSaved(AiConversationMessage message) {
			}

			@Override
			public void publishAssistantGenerationStarted(UUID conversationId) {
			}

			@Override
			public void publishAssistantMessageCreated(AiConversationMessage message) {
			}

			@Override
			public void publishAssistantGenerationFailed(UUID conversationId, String errorCode) {
			}
		};
	}
}
