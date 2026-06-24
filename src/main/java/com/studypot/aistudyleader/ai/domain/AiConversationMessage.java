package com.studypot.aistudyleader.ai.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record AiConversationMessage(
	UUID id,
	UUID conversationId,
	UUID llmUsageId,
	AiConversationMessageSenderType senderType,
	String content,
	Map<String, Object> metadata,
	Instant createdAt
) {

	public AiConversationMessage {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(conversationId, "conversationId must not be null");
		Objects.requireNonNull(senderType, "senderType must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		content = requiredContent(content);
		metadata = Collections.unmodifiableMap(new LinkedHashMap<>(metadata == null ? Map.of() : metadata));
	}

	public static AiConversationMessage userMessage(UUID id, UUID conversationId, String content, Instant createdAt) {
		return new AiConversationMessage(
			id,
			conversationId,
			null,
			AiConversationMessageSenderType.USER,
			content,
			Map.of(),
			createdAt
		);
	}

	public static AiConversationMessage assistantSeedMessage(
		UUID id,
		UUID conversationId,
		String content,
		Map<String, Object> metadata,
		Instant createdAt
	) {
		return new AiConversationMessage(
			id,
			conversationId,
			null,
			AiConversationMessageSenderType.ASSISTANT,
			content,
			metadata,
			createdAt
		);
	}

	public static AiConversationMessage assistantMessage(
		UUID id,
		UUID conversationId,
		UUID llmUsageId,
		String content,
		Map<String, Object> metadata,
		Instant createdAt
	) {
		Objects.requireNonNull(llmUsageId, "llmUsageId must not be null");
		return new AiConversationMessage(
			id,
			conversationId,
			llmUsageId,
			AiConversationMessageSenderType.ASSISTANT,
			content,
			metadata,
			createdAt
		);
	}

	public AiConversationMessage withMetadata(Map<String, Object> metadata) {
		return new AiConversationMessage(id, conversationId, llmUsageId, senderType, content, metadata, createdAt);
	}

	private static String requiredContent(String value) {
		String normalized = value == null ? "" : value.strip();
		if (normalized.isBlank()) {
			throw new IllegalArgumentException("content must not be blank.");
		}
		return normalized;
	}
}
