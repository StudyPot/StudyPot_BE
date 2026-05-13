package com.studypot.aistudyleader.llm.service;

import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import java.util.Map;
import java.util.Objects;

public record LlmStructuredRequest(
	LlmUsagePurpose purpose,
	String instructions,
	Map<String, Object> input,
	Map<String, Object> textFormat,
	Map<String, Object> requestPayload
) {

	public LlmStructuredRequest {
		Objects.requireNonNull(purpose, "purpose must not be null");
		instructions = requireText(instructions, "instructions");
		input = Map.copyOf(Objects.requireNonNull(input, "input must not be null"));
		textFormat = Map.copyOf(Objects.requireNonNull(textFormat, "textFormat must not be null"));
		requestPayload = Map.copyOf(Objects.requireNonNull(requestPayload, "requestPayload must not be null"));
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.strip();
	}
}
