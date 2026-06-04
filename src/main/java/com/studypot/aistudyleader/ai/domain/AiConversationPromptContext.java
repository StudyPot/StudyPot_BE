package com.studypot.aistudyleader.ai.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AiConversationPromptContext(
	Map<String, Object> studyGroup,
	Map<String, Object> curriculum,
	Map<String, Object> conversation,
	List<Map<String, Object>> messages,
	Map<String, Object> week,
	List<Map<String, Object>> tasks,
	Map<String, Object> progress,
	Map<String, Object> retrospective
) {

	public AiConversationPromptContext {
		studyGroup = copyMap(studyGroup);
		curriculum = copyMap(curriculum);
		conversation = copyMap(conversation);
		messages = copyList(messages);
		week = copyMap(week);
		tasks = copyList(tasks);
		progress = copyMap(progress);
		retrospective = copyMap(retrospective);
	}

	public static AiConversationPromptContext empty() {
		return new AiConversationPromptContext(
			Map.of("status", "NOT_AVAILABLE"),
			Map.of("status", "NOT_AVAILABLE"),
			Map.of("status", "NOT_AVAILABLE"),
			List.of(),
			Map.of("status", "NOT_AVAILABLE"),
			List.of(),
			Map.of("status", "NOT_AVAILABLE"),
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
			result.add(copyMap(Objects.requireNonNull(item, "AI conversation prompt context item must not be null")));
		}
		return Collections.unmodifiableList(result);
	}

	private static Map<String, Object> copyMapValues(Map<?, ?> source) {
		Map<String, Object> result = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : source.entrySet()) {
			Object key = entry.getKey();
			if (!(key instanceof String stringKey) || stringKey.isBlank()) {
				throw new IllegalArgumentException("AI conversation prompt context map keys must be non-blank strings.");
			}
			result.put(stringKey, copyValue(entry.getValue()));
		}
		return Collections.unmodifiableMap(result);
	}

	private static Object copyValue(Object value) {
		if (value == null) {
			throw new IllegalArgumentException("AI conversation prompt context map must not contain null values.");
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
