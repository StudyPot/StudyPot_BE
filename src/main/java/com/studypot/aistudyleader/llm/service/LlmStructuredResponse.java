package com.studypot.aistudyleader.llm.service;

import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsage;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record LlmStructuredResponse(
	LlmProvider provider,
	String model,
	String outputText,
	int inputTokens,
	int outputTokens,
	BigDecimal totalCostUsd,
	Integer latencyMs,
	LlmUsageStatus status,
	String errorCode,
	Map<String, Object> requestPayload,
	String responseSummary
) {

	public LlmStructuredResponse {
		Objects.requireNonNull(provider, "provider must not be null");
		model = requireText(model, "model");
		outputText = requireText(outputText, "outputText");
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
	}

	public LlmCallFailure toFailure(LlmUsagePurpose purpose, String errorCode, String responseSummary) {
		return new LlmCallFailure(
			purpose,
			provider,
			model,
			inputTokens,
			outputTokens,
			totalCostUsd,
			latencyMs,
			LlmUsageStatus.FAILED,
			errorCode,
			requestPayload,
			responseSummary
		);
	}

	public LlmStructuredResponse withResponseSummary(String responseSummary) {
		return new LlmStructuredResponse(
			provider,
			model,
			outputText,
			inputTokens,
			outputTokens,
			totalCostUsd,
			latencyMs,
			status,
			errorCode,
			requestPayload,
			responseSummary
		);
	}

	public LlmUsage toUsage(UUID id, UUID userId, UUID groupId, LlmUsagePurpose purpose, Instant now) {
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
