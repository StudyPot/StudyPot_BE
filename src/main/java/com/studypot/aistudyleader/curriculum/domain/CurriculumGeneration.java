package com.studypot.aistudyleader.curriculum.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record CurriculumGeneration(
	String title,
	List<CurriculumWeekPlan> weeks,
	String generationPrompt,
	LlmProvider provider,
	String model,
	int inputTokens,
	int outputTokens,
	BigDecimal totalCostUsd,
	Integer latencyMs,
	LlmUsageStatus status,
	String errorCode,
	Map<String, Object> requestPayload,
	String responseSummary
) {

	public CurriculumGeneration {
		title = requireText(title, "title");
		weeks = List.copyOf(Objects.requireNonNull(weeks, "weeks must not be null"));
		if (weeks.isEmpty()) {
			throw new IllegalArgumentException("weeks must not be empty");
		}
		generationPrompt = generationPrompt == null || generationPrompt.isBlank() ? null : generationPrompt.strip();
		Objects.requireNonNull(provider, "provider must not be null");
		model = requireText(model, "model");
		if (inputTokens < 0 || outputTokens < 0) {
			throw new IllegalArgumentException("token counts must not be negative");
		}
		Objects.requireNonNull(status, "status must not be null");
		errorCode = errorCode == null || errorCode.isBlank() ? null : errorCode.strip();
		requestPayload = Map.copyOf(Objects.requireNonNull(requestPayload, "requestPayload must not be null"));
		responseSummary = responseSummary == null || responseSummary.isBlank() ? null : responseSummary.strip();
	}

	public LlmUsage toLlmUsage(UUID id, UUID userId, UUID groupId, Instant now) {
		return new LlmUsage(
			id,
			userId,
			groupId,
			"CURRICULUM_GENERATE",
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

	public Curriculum toCurriculum(
		UUID curriculumId,
		UUID groupId,
		UUID llmUsageId,
		Map<String, Object> onboardingSummary,
		Instant now,
		List<UUID> weekIds,
		List<UUID> taskIds
	) {
		if (weekIds.size() != weeks.size()) {
			throw new IllegalArgumentException("weekIds size must match weeks size");
		}
		List<CurriculumWeek> builtWeeks = new ArrayList<>();
		int taskIndex = 0;
		Instant weekStart = now;
		for (int i = 0; i < weeks.size(); i++) {
			CurriculumWeekPlan week = weeks.get(i);
			Instant weekEnd = weekStart.plus(Duration.ofDays(7));
			List<WeeklyTask> tasks = new ArrayList<>();
			for (int j = 0; j < week.tasks().size(); j++) {
				if (taskIndex >= taskIds.size()) {
					throw new IllegalArgumentException("taskIds size must match generated tasks size");
				}
				CurriculumTaskPlan task = week.tasks().get(j);
				tasks.add(new WeeklyTask(
					taskIds.get(taskIndex),
					weekIds.get(i),
					j + 1,
					task.taskType(),
					task.title(),
					task.description(),
					task.required(),
					weekEnd,
					true,
					Map.of("weekNumber", week.weekNumber(), "displayOrder", j + 1),
					now,
					now
				));
				taskIndex++;
			}
			builtWeeks.add(new CurriculumWeek(
				weekIds.get(i),
				curriculumId,
				week.weekNumber(),
				week.title(),
				null,
				week.sprintGoal(),
				week.learningGoals(),
				week.resources(),
				i == 0 ? CurriculumWeekStatus.IN_PROGRESS : CurriculumWeekStatus.PENDING,
				weekStart,
				weekEnd,
				tasks,
				now,
				now
			));
			weekStart = weekEnd;
		}
		if (taskIndex != taskIds.size()) {
			throw new IllegalArgumentException("taskIds size must match generated tasks size");
		}
		return new Curriculum(
			curriculumId,
			groupId,
			llmUsageId,
			title,
			weeks.size(),
			onboardingSummary,
			true,
			generationPrompt,
			CurriculumStatus.ACTIVE,
			builtWeeks,
			now,
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
