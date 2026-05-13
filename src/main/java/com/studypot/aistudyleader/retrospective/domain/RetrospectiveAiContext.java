package com.studypot.aistudyleader.retrospective.domain;

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
		for (Map.Entry<String, Object> entry : source.entrySet()) {
			if (entry.getKey() == null || entry.getValue() == null) {
				throw new IllegalArgumentException("retrospective AI context map must not contain null keys or values.");
			}
		}
		return Map.copyOf(source);
	}

	private static List<Map<String, Object>> copyList(List<Map<String, Object>> source) {
		if (source == null || source.isEmpty()) {
			return List.of();
		}
		return source.stream()
			.map(item -> copyMap(Objects.requireNonNull(item, "retrospective AI context list item must not be null")))
			.toList();
	}
}
