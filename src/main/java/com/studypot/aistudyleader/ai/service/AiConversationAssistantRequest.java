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
	AiConversationPromptContext promptContext,
	String userPlan
) {

	public AiConversationAssistantRequest {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(messageContext, "messageContext must not be null");
		Objects.requireNonNull(userMessage, "userMessage must not be null");
		Objects.requireNonNull(promptContext, "promptContext must not be null");
		userPlan = userPlan == null || userPlan.isBlank() ? null : userPlan.strip();
	}

	/** 기존 호출부 호환용(플랜 미지정 = null). */
	public AiConversationAssistantRequest(
		UUID authenticatedUserId,
		AiConversationMessageContext messageContext,
		AiConversationMessage userMessage,
		AiConversationPromptContext promptContext
	) {
		this(authenticatedUserId, messageContext, userMessage, promptContext, null);
	}
}
