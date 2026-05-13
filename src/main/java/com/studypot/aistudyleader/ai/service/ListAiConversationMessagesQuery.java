package com.studypot.aistudyleader.ai.service;

import java.util.Objects;
import java.util.UUID;

public record ListAiConversationMessagesQuery(
	UUID authenticatedUserId,
	UUID conversationId,
	String cursor,
	int pageSize
) {

	public static final int MAX_PAGE_SIZE = 100;

	public ListAiConversationMessagesQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(conversationId, "conversationId must not be null");
		if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
			throw new InvalidAiConversationRequestException("pageSize", "pageSize must be between 1 and 100.");
		}
		cursor = cursor == null || cursor.isBlank() ? null : cursor.strip();
	}
}
