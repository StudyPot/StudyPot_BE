package com.studypot.aistudyleader.llm.service;

import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import java.util.Map;
import java.util.Objects;

public record LlmStructuredRequest(
	LlmUsagePurpose purpose,
	String instructions,
	Map<String, Object> input,
	Map<String, Object> textFormat,
	Map<String, Object> requestPayload,
	String userPlan
) {

	public LlmStructuredRequest {
		Objects.requireNonNull(purpose, "purpose must not be null");
		instructions = requireText(instructions, "instructions");
		input = copyMap(input, "input");
		textFormat = copyMap(textFormat, "textFormat");
		requestPayload = copyMap(requestPayload, "requestPayload");
		userPlan = userPlan == null || userPlan.isBlank() ? null : userPlan.strip();
	}

	/** 기존 호출부 호환용: 플랜 정보가 없는(또는 불필요한) 호출은 userPlan 을 null 로 둔다. */
	public LlmStructuredRequest(
		LlmUsagePurpose purpose,
		String instructions,
		Map<String, Object> input,
		Map<String, Object> textFormat,
		Map<String, Object> requestPayload
	) {
		this(purpose, instructions, input, textFormat, requestPayload, null);
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
