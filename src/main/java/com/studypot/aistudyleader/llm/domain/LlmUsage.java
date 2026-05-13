package com.studypot.aistudyleader.llm.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record LlmUsage(
	UUID id,
	UUID userId,
	UUID groupId,
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
	String responseSummary,
	LocalDate createdDateUtc,
	Instant createdAt
) {

	private static final int MAX_REQUEST_TEXT_LENGTH = 512;
	private static final String REDACTED = "[REDACTED]";
	private static final String TRUNCATED_SUFFIX = "...[TRUNCATED]";

	public LlmUsage {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(purpose, "purpose must not be null");
		Objects.requireNonNull(provider, "provider must not be null");
		model = requireText(model, "model");
		if (inputTokens < 0 || outputTokens < 0) {
			throw new IllegalArgumentException("token counts must not be negative.");
		}
		if (totalCostUsd != null && totalCostUsd.signum() < 0) {
			throw new IllegalArgumentException("totalCostUsd must not be negative.");
		}
		if (latencyMs != null && latencyMs < 0) {
			throw new IllegalArgumentException("latencyMs must not be negative.");
		}
		Objects.requireNonNull(status, "status must not be null");
		errorCode = normalizeOptionalText(errorCode);
		requestPayload = redactMap(requestPayload == null ? Map.of() : requestPayload);
		responseSummary = normalizeOptionalText(responseSummary);
		Objects.requireNonNull(createdDateUtc, "createdDateUtc must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
	}

	public static LlmUsage record(
		UUID id,
		UUID userId,
		UUID groupId,
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
		String responseSummary,
		Instant now
	) {
		Objects.requireNonNull(now, "now must not be null");
		return new LlmUsage(
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
			LocalDate.ofInstant(now, ZoneOffset.UTC),
			now
		);
	}

	private static Map<String, Object> redactMap(Map<String, Object> source) {
		if (source.isEmpty()) {
			return Map.of();
		}
		Map<String, Object> redacted = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : source.entrySet()) {
			String key = requireText(entry.getKey(), "requestPayload key");
			redacted.put(key, sensitiveKey(key) ? REDACTED : redactValue(entry.getValue()));
		}
		return Collections.unmodifiableMap(redacted);
	}

	private static Object redactValue(Object value) {
		if (value == null) {
			return null;
		}
		return switch (value) {
			case Map<?, ?> map -> redactUntypedMap(map);
			case List<?> list -> redactList(list);
			case String text -> truncate(text.strip());
			case Number ignored -> value;
			case Boolean ignored -> value;
			default -> truncate(value.toString());
		};
	}

	private static Map<String, Object> redactUntypedMap(Map<?, ?> source) {
		Map<String, Object> typed = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : source.entrySet()) {
			if (!(entry.getKey() instanceof String key)) {
				throw new IllegalArgumentException("requestPayload key must be a string.");
			}
			typed.put(key, sensitiveKey(key) ? REDACTED : redactValue(entry.getValue()));
		}
		return redactMap(typed);
	}

	private static List<Object> redactList(List<?> source) {
		List<Object> result = new ArrayList<>();
		for (Object item : source) {
			result.add(redactValue(item));
		}
		return Collections.unmodifiableList(result);
	}

	private static boolean sensitiveKey(String key) {
		String normalized = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
		return normalized.contains("token")
			|| normalized.contains("secret")
			|| normalized.contains("apikey")
			|| normalized.contains("authorization")
			|| normalized.contains("oauth")
			|| normalized.contains("cookie")
			|| normalized.contains("password")
			|| normalized.contains("credential");
	}

	private static String truncate(String value) {
		if (value.length() <= MAX_REQUEST_TEXT_LENGTH) {
			return value;
		}
		return value.substring(0, MAX_REQUEST_TEXT_LENGTH) + TRUNCATED_SUFFIX;
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank.");
		}
		return value.strip();
	}

	private static String normalizeOptionalText(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}
