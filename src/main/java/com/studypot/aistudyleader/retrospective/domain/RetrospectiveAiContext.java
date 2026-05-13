package com.studypot.aistudyleader.retrospective.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RetrospectiveAiContext(
	Map<String, Object> onboarding,
	List<Map<String, Object>> rules,
	List<Map<String, Object>> ruleViolations,
	List<Map<String, Object>> priorRetrospectives,
	Map<String, Object> conversationSummary
) {

	public RetrospectiveAiContext {
		onboarding = copyMap(onboarding);
		rules = copyList(rules);
		ruleViolations = copyList(ruleViolations);
		priorRetrospectives = copyList(priorRetrospectives);
		conversationSummary = copyMap(conversationSummary);
	}

	public static RetrospectiveAiContext empty() {
		return new RetrospectiveAiContext(
			Map.of("status", "NOT_AVAILABLE"),
			List.of(),
			List.of(),
			List.of(),
			Map.of("status", "NOT_AVAILABLE")
		);
	}

	private static Map<String, Object> copyMap(Map<String, Object> source) {
		if (source == null || source.isEmpty()) {
			return Map.of();
		}
		return copyMapValues(source);
	}

	private static List<Map<String, Object>> copyList(List<Map<String, Object>> source) {
		if (source == null || source.isEmpty()) {
			return List.of();
		}
		List<Map<String, Object>> result = new ArrayList<>(source.size());
		for (Map<String, Object> item : source) {
			result.add(copyMap(Objects.requireNonNull(item, "retrospective AI context list item must not be null")));
		}
		return Collections.unmodifiableList(result);
	}

	private static Map<String, Object> copyMapValues(Map<?, ?> source) {
		Map<String, Object> result = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : source.entrySet()) {
			Object key = entry.getKey();
			if (!(key instanceof String stringKey) || stringKey.isBlank()) {
				throw new IllegalArgumentException("retrospective AI context map keys must be non-blank strings.");
			}
			result.put(stringKey, copyValue(entry.getValue()));
		}
		return Collections.unmodifiableMap(result);
	}

	private static Object copyValue(Object value) {
		if (value == null) {
			throw new IllegalArgumentException("retrospective AI context map must not contain null values.");
		}
		if (value instanceof Map<?, ?> nestedMap) {
			return copyMapValues(nestedMap);
		}
		if (value instanceof List<?> nestedList) {
			List<Object> result = new ArrayList<>(nestedList.size());
			for (Object item : nestedList) {
				result.add(copyValue(item));
			}
			return Collections.unmodifiableList(result);
		}
		return value;
	}
}
