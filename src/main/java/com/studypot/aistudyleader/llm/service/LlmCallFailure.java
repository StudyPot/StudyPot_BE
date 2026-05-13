package com.studypot.aistudyleader.llm.service;

import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsage;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record LlmCallFailure(
	LlmUsagePurpose purpose,
	LlmProvider provider,
	String model,
	int inputTokens,
	int outputTokens,
	BigDecimal totalCostUsd,
	Integer latencyMs,
	LlmUsageStatus status,
	String errorCode,
	Map<String, Object> requestPayload,
	String responseSummary
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	public LlmCallFailure {
		Objects.requireNonNull(purpose, "purpose must not be null");
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
		if (status == LlmUsageStatus.SUCCESS) {
			throw new IllegalArgumentException("failure status must not be SUCCESS");
		}
		errorCode = requireText(errorCode, "errorCode");
		requestPayload = Map.copyOf(Objects.requireNonNull(requestPayload, "requestPayload must not be null"));
		responseSummary = responseSummary == null || responseSummary.isBlank() ? null : responseSummary.strip();
	}

	public LlmUsage toUsage(UUID id, UUID userId, UUID groupId, Instant now) {
		return LlmUsage.record(
			id,
			userId,
			groupId,
			purpose,
			provider,
			model,
			inputTokens,
			outputTokens,
			totalCostUsd,
			latencyMs,
			status,
			errorCode,
			requestPayload,
			responseSummary,
			now
		);
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.strip();
	}
}
