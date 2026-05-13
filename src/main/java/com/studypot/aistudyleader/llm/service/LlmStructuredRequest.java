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
		input = copyMap(input, "input");
		textFormat = copyMap(textFormat, "textFormat");
		requestPayload = copyMap(requestPayload, "requestPayload");
	}

	private static Map<String, Object> copyMap(Map<String, Object> value, String fieldName) {
		Objects.requireNonNull(value, fieldName + " must not be null");
		for (Map.Entry<String, Object> entry : value.entrySet()) {
			if (entry.getKey() == null || entry.getValue() == null) {
				throw new IllegalArgumentException(fieldName + " must not contain null key or value");
			}
		}
		return Map.copyOf(value);
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.strip();
	}
}
