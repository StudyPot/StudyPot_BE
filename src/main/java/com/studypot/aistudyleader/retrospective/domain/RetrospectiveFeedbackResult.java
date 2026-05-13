package com.studypot.aistudyleader.retrospective.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record RetrospectiveFeedbackResult(
	Map<String, Object> aiFeedback,
	Map<String, Object> nextWeekAdjustment
) {

	private static final Set<String> NEXT_WEEK_ADJUSTMENT_KEYS = Set.of(
		"difficulty",
		"taskChanges",
		"supportMaterials",
		"memberNotes"
	);

	public RetrospectiveFeedbackResult {
		aiFeedback = immutableMap(Objects.requireNonNull(aiFeedback, "aiFeedback must not be null"));
		nextWeekAdjustment = validateNextWeekAdjustment(nextWeekAdjustment == null ? Map.of() : nextWeekAdjustment);
	}

	public static RetrospectiveFeedbackResult of(
		String summary,
		List<String> strengths,
		List<String> risks,
		List<String> actionItems,
		Map<String, Object> nextWeekAdjustment
	) {
		Map<String, Object> aiFeedback = new LinkedHashMap<>();
		aiFeedback.put("summary", requiredText("summary", summary));
		aiFeedback.put("strengths", stringList("strengths", strengths));
		aiFeedback.put("risks", stringList("risks", risks));
		aiFeedback.put("actionItems", stringList("actionItems", actionItems));
		return new RetrospectiveFeedbackResult(aiFeedback, nextWeekAdjustment);
	}

	private static Map<String, Object> validateNextWeekAdjustment(Map<String, Object> value) {
		Map<String, Object> result = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : value.entrySet()) {
			String key = entry.getKey();
			if (!NEXT_WEEK_ADJUSTMENT_KEYS.contains(key)) {
				throw new IllegalArgumentException("nextWeekAdjustment contains unsupported key: " + key);
			}
			Object entryValue = entry.getValue();
			if (entryValue == null) {
				throw new IllegalArgumentException("nextWeekAdjustment." + key + " must not be null.");
			}
			result.put(key, validateNextWeekAdjustmentValue(key, entryValue));
		}
		return immutableMap(result);
	}

	private static Object validateNextWeekAdjustmentValue(String key, Object value) {
		return switch (key) {
			case "difficulty" -> requiredText("nextWeekAdjustment.difficulty", value);
			case "taskChanges", "supportMaterials" -> objectStringList("nextWeekAdjustment." + key, value);
			case "memberNotes" -> memberNotes(value);
			default -> throw new IllegalArgumentException("nextWeekAdjustment contains unsupported key: " + key);
		};
	}

	private static List<Map<String, Object>> memberNotes(Object value) {
		if (!(value instanceof List<?> rawList)) {
			throw new IllegalArgumentException("nextWeekAdjustment.memberNotes must be a list of objects.");
		}
		List<Map<String, Object>> notes = new ArrayList<>();
		for (Object item : rawList) {
			if (!(item instanceof Map<?, ?> rawMap)) {
				throw new IllegalArgumentException("nextWeekAdjustment.memberNotes must be a list of objects.");
			}
			Map<String, Object> note = new LinkedHashMap<>();
			for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
				if (!(entry.getKey() instanceof String key)) {
					throw new IllegalArgumentException("nextWeekAdjustment.memberNotes keys must be strings.");
				}
				Object entryValue = entry.getValue();
				if (!(entryValue instanceof String text) || text.isBlank()) {
					throw new IllegalArgumentException("nextWeekAdjustment.memberNotes." + key + " must be a non-blank string.");
				}
				note.put(key, text.strip());
			}
			notes.add(immutableMap(note));
		}
		return List.copyOf(notes);
	}

	private static List<String> objectStringList(String fieldName, Object value) {
		if (!(value instanceof List<?> list)) {
			throw new IllegalArgumentException(fieldName + " must be a list of strings.");
		}
		List<String> result = new ArrayList<>();
		for (Object item : list) {
			result.add(requiredText(fieldName, item));
		}
		return List.copyOf(result);
	}

	private static List<String> stringList(String fieldName, List<String> value) {
		if (value == null) {
			return List.of();
		}
		List<String> result = new ArrayList<>();
		for (String item : value) {
			result.add(requiredText(fieldName, item));
		}
		return List.copyOf(result);
	}

	private static String requiredText(String fieldName, Object value) {
		if (!(value instanceof String text) || text.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank.");
		}
		return text.strip();
	}

	private static Map<String, Object> immutableMap(Map<String, Object> value) {
		if (value.isEmpty()) {
			return Map.of();
		}
		return Collections.unmodifiableMap(new LinkedHashMap<>(value));
	}
}
