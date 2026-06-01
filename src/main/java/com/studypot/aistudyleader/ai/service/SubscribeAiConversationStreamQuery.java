package com.studypot.aistudyleader.ai.service;

import java.util.Objects;
import java.util.UUID;

public record SubscribeAiConversationStreamQuery(UUID authenticatedUserId, UUID conversationId) {

	public SubscribeAiConversationStreamQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(conversationId, "conversationId must not be null");
	}
}
