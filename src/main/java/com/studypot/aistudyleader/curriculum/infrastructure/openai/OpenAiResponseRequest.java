package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import java.util.Map;
import java.util.Objects;

public record OpenAiResponseRequest(Map<String, Object> body) {

	public OpenAiResponseRequest {
		body = Map.copyOf(Objects.requireNonNull(body, "body must not be null"));
	}
}
