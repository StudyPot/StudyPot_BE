package com.studypot.aistudyleader.ai.service;

import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record AiConversationAssistantResponse(
	String message,
	String conversationSummaryPatch,
	Map<String, Object> metadata,
	LlmStructuredResponse llmResponse
) {

	public AiConversationAssistantResponse {
		message = requireText(message, "message");
		conversationSummaryPatch = conversationSummaryPatch == null || conversationSummaryPatch.isBlank()
			? null
			: conversationSummaryPatch.strip();
		metadata = Map.copyOf(metadata == null ? Map.of() : new LinkedHashMap<>(metadata));
		Objects.requireNonNull(llmResponse, "llmResponse must not be null");
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank.");
		}
		return value.strip();
	}
}
