package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import java.util.Map;
import java.util.Objects;

public record OpenAiResponseRequest(String path, Map<String, Object> body) {

	public OpenAiResponseRequest(Map<String, Object> body) {
		this("/responses", body);
	}

	public OpenAiResponseRequest {
		if (path == null || path.isBlank()) {
			throw new IllegalArgumentException("path must not be blank");
		}
		path = path.strip();
		if (!path.startsWith("/")) {
			throw new IllegalArgumentException("path must start with /");
		}
		body = Map.copyOf(Objects.requireNonNull(body, "body must not be null"));
	}
}
