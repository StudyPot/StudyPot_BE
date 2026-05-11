package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "studypot.ai.openai")
record OpenAiCurriculumProperties(
	String apiKey,
	String baseUrl,
	String model
) {

	private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
	private static final String DEFAULT_MODEL = "gpt-4o-mini";

	OpenAiCurriculumProperties {
		apiKey = blankToNull(apiKey);
		baseUrl = blankToDefault(baseUrl, DEFAULT_BASE_URL);
		model = blankToDefault(model, DEFAULT_MODEL);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

	private static String blankToDefault(String value, String defaultValue) {
		return value == null || value.isBlank() ? defaultValue : value.strip();
	}
}
