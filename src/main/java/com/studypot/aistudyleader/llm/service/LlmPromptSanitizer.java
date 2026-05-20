package com.studypot.aistudyleader.llm.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LlmPromptSanitizer {

	public static final String REDACTED = "[REDACTED]";

	private static final String SENSITIVE_ASSIGNMENT_KEYS = String.join(
		"|",
		"authorization",
		"api[_-]?key",
		"provider[_-]?key",
		"private[_-]?key",
		"private[_-]?note",
		"access[_-]?token",
		"refresh[_-]?token",
		"oauth[_-]?token",
		"cookie",
		"credential",
		"secret",
		"password"
	);
	private static final Pattern CREDENTIAL_ASSIGNMENT = Pattern.compile(
		"(?i)\\b(" + SENSITIVE_ASSIGNMENT_KEYS + ")\\b\\s*[:=]\\s*(Bearer\\s+)?[^\\s,;]+"
	);

	private LlmPromptSanitizer() {
	}

	public static Map<String, Object> sanitizeMap(Map<String, Object> source) {
		Objects.requireNonNull(source, "source must not be null");
		return sanitizeUntypedMap(source);
	}

	private static Object sanitizeValue(Object value) {
		if (value == null) {
			return null;
		}
		return switch (value) {
			case Map<?, ?> map -> sanitizeUntypedMap(map);
			case List<?> list -> sanitizeList(list);
			case String text -> redactCredentialAssignments(text.strip());
			case Number ignored -> value;
			case Boolean ignored -> value;
			default -> redactCredentialAssignments(value.toString().strip());
		};
	}

	private static Map<String, Object> sanitizeUntypedMap(Map<?, ?> source) {
		if (source.isEmpty()) {
			return Map.of();
		}
		Map<String, Object> sanitized = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : source.entrySet()) {
			if (!(entry.getKey() instanceof String key) || key.isBlank()) {
				throw new IllegalArgumentException("prompt input key must be a non-blank string.");
			}
			sanitized.put(key, sensitiveKey(key) ? REDACTED : sanitizeValue(entry.getValue()));
		}
		return Collections.unmodifiableMap(sanitized);
	}

	private static List<Object> sanitizeList(List<?> source) {
		if (source.isEmpty()) {
			return List.of();
		}
		List<Object> sanitized = new ArrayList<>();
		for (Object item : source) {
			sanitized.add(sanitizeValue(item));
		}
		return Collections.unmodifiableList(sanitized);
	}

	private static String redactCredentialAssignments(String text) {
		Matcher matcher = CREDENTIAL_ASSIGNMENT.matcher(text);
		StringBuffer result = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(1) + "=" + REDACTED));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private static boolean sensitiveKey(String key) {
		String normalized = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
		return normalized.contains("token")
			|| normalized.contains("secret")
			|| normalized.contains("apikey")
			|| normalized.contains("providerkey")
			|| normalized.contains("privatekey")
			|| normalized.contains("privatenote")
			|| normalized.contains("authorization")
			|| normalized.contains("oauth")
			|| normalized.contains("cookie")
			|| normalized.contains("password")
			|| normalized.contains("credential");
	}
}
