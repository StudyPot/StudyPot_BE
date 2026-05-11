package com.studypot.aistudyleader.curriculum.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record LlmUsage(
	UUID id,
	UUID userId,
	UUID groupId,
	String purpose,
	LlmProvider provider,
	String model,
	int inputTokens,
	int outputTokens,
	BigDecimal totalCostUsd,
	Integer latencyMs,
	LlmUsageStatus status,
	String errorCode,
	Map<String, Object> requestPayload,
	String responseSummary,
	LocalDate createdDateUtc,
	Instant createdAt
) {

	public LlmUsage {
		Objects.requireNonNull(id, "id must not be null");
		purpose = requireText(purpose, "purpose");
		Objects.requireNonNull(provider, "provider must not be null");
		model = requireText(model, "model");
		if (inputTokens < 0 || outputTokens < 0) {
			throw new IllegalArgumentException("token counts must not be negative");
		}
		if (totalCostUsd != null && totalCostUsd.signum() < 0) {
			throw new IllegalArgumentException("totalCostUsd must not be negative");
		}
		if (latencyMs != null && latencyMs < 0) {
			throw new IllegalArgumentException("latencyMs must not be negative");
		}
		Objects.requireNonNull(status, "status must not be null");
		errorCode = errorCode == null || errorCode.isBlank() ? null : errorCode.strip();
		requestPayload = Map.copyOf(Objects.requireNonNull(requestPayload, "requestPayload must not be null"));
		responseSummary = responseSummary == null || responseSummary.isBlank() ? null : responseSummary.strip();
		Objects.requireNonNull(createdDateUtc, "createdDateUtc must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.strip();
	}
}
