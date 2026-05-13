package com.studypot.aistudyleader.ai.service;

import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageContext;
import com.studypot.aistudyleader.ai.domain.AiConversationPromptContext;
import java.util.Objects;
import java.util.UUID;

public record AiConversationAssistantRequest(
	UUID authenticatedUserId,
	AiConversationMessageContext messageContext,
	AiConversationMessage userMessage,
	AiConversationPromptContext promptContext
) {

	public AiConversationAssistantRequest {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(messageContext, "messageContext must not be null");
		Objects.requireNonNull(userMessage, "userMessage must not be null");
		Objects.requireNonNull(promptContext, "promptContext must not be null");
	}
}
