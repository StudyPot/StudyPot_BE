package com.studypot.aistudyleader.retrospective.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record Retrospective(
	UUID id,
	UUID progressId,
	UUID curriculumWeekId,
	UUID memberId,
	UUID llmUsageId,
	RetrospectiveTriggerType triggerType,
	Map<String, Object> inputSummary,
	Map<String, Object> aiFeedback,
	Map<String, Object> nextWeekAdjustment,
	RetrospectiveStatus status,
	Instant requestedAt,
	Instant completedAt,
	Instant createdAt,
	Instant updatedAt
) {

	public Retrospective {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(progressId, "progressId must not be null");
		Objects.requireNonNull(curriculumWeekId, "curriculumWeekId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(triggerType, "triggerType must not be null");
		inputSummary = immutableMap(inputSummary);
		aiFeedback = immutableMap(aiFeedback);
		nextWeekAdjustment = immutableMap(nextWeekAdjustment);
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(requestedAt, "requestedAt must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	public static Retrospective requested(
		UUID id,
		UUID progressId,
		UUID curriculumWeekId,
		UUID memberId,
		RetrospectiveTriggerType triggerType,
		Map<String, Object> inputSummary,
		Instant now
	) {
		Objects.requireNonNull(now, "now must not be null");
		return new Retrospective(
			id,
			progressId,
			curriculumWeekId,
			memberId,
			null,
			triggerType,
			requiredInputSummary(inputSummary),
			Map.of(),
			Map.of(),
			RetrospectiveStatus.PENDING,
			now,
			null,
			now,
			now
		);
	}

	private static Map<String, Object> requiredInputSummary(Map<String, Object> inputSummary) {
		if (inputSummary == null || inputSummary.isEmpty()) {
			throw new IllegalArgumentException("inputSummary must not be empty.");
		}
		return immutableMap("inputSummary", inputSummary);
	}

	private static Map<String, Object> immutableMap(Map<String, Object> value) {
		return immutableMap("JSON map", value);
	}

	private static Map<String, Object> immutableMap(String fieldName, Map<String, Object> value) {
		if (value == null || value.isEmpty()) {
			return Map.of();
		}
		for (Map.Entry<String, Object> entry : value.entrySet()) {
			if (entry.getKey() == null || entry.getValue() == null) {
				throw new IllegalArgumentException(fieldName + " must not contain null keys or values.");
			}
		}
		return Collections.unmodifiableMap(new LinkedHashMap<>(value));
	}
}
