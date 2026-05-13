package com.studypot.aistudyleader.ai.service;

import java.util.Objects;
import java.util.UUID;

public record SendAiConversationMessageCommand(
	UUID authenticatedUserId,
	UUID conversationId,
	String content
) {

	public SendAiConversationMessageCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(conversationId, "conversationId must not be null");
		content = content == null ? "" : content.strip();
		if (content.isBlank()) {
			throw new InvalidAiConversationRequestException("content", "content must not be blank.");
		}
	}
}
