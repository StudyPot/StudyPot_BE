package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "studypot.ai.openai")
record OpenAiCurriculumProperties(
	String apiKey,
	String baseUrl,
	String model,
	OpenAiPurposeModels models,
	OpenAiApiMode apiMode,
	Duration connectTimeout,
	Duration readTimeout,
	OpenAiOutputTokenLimits outputTokenLimits,
	OpenAiReasoningEfforts reasoningEfforts
) {

	private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
	private static final String DEFAULT_MODEL = "gpt-4o-mini";
	private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(120);

	OpenAiCurriculumProperties {
		apiKey = blankToNull(apiKey);
		baseUrl = blankToDefault(baseUrl, DEFAULT_BASE_URL);
		model = blankToDefault(model, DEFAULT_MODEL);
		models = models == null ? OpenAiPurposeModels.none() : models;
		apiMode = apiMode == null ? OpenAiApiMode.RESPONSES : apiMode;
		connectTimeout = positiveOrDefault(connectTimeout, DEFAULT_CONNECT_TIMEOUT, "connectTimeout");
		readTimeout = positiveOrDefault(readTimeout, DEFAULT_READ_TIMEOUT, "readTimeout");
		outputTokenLimits = outputTokenLimits == null ? OpenAiOutputTokenLimits.defaults() : outputTokenLimits;
		reasoningEfforts = reasoningEfforts == null ? OpenAiReasoningEfforts.defaults() : reasoningEfforts;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

	private static String blankToDefault(String value, String defaultValue) {
		return value == null || value.isBlank() ? defaultValue : value.strip();
	}

	private static Duration positiveOrDefault(Duration value, Duration defaultValue, String fieldName) {
		Duration resolved = value == null ? defaultValue : value;
		if (resolved.isZero() || resolved.isNegative()) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		return resolved;
	}
}
