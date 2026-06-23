package com.studypot.aistudyleader.curriculum.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.curriculum.domain.CurriculumTaskPlan;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.service.LlmProviderCallException;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmPromptSanitizer;
import com.studypot.aistudyleader.llm.service.LlmStructuredRequest;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class ProviderBackedNextWeekPlanGenerator implements NextWeekPlanGenerator {

	private static final String INSTRUCTIONS = """
		직전 주차의 학습 리포트를 바탕으로 '다음 주차' 학습 TODO 목록과 회고 질문 프롬프트를 한국어로 작성하세요.
		- tasks: 다음 주차에 수행할 구체적 TODO 3~6개. 각 항목은 taskType(READING/PRACTICE/ASSIGNMENT/PROJECT/CUSTOM),
		  title, description, required(boolean) 를 포함합니다.
		- retrospectivePrompt: 다음 주차 회고를 작성할 때 보여줄 회고 유도 질문 2~4개(줄바꿈 구분).
		비밀키, OAuth, 자격증명류 값은 절대 포함하지 마세요. 제공된 JSON 스키마에 맞는 JSON 만 반환하세요.
		""";

	private static final List<String> TASK_TYPES = List.of("READING", "PRACTICE", "ASSIGNMENT", "PROJECT", "CUSTOM");

	private final LlmProviderClient provider;
	private final ObjectMapper objectMapper;

	ProviderBackedNextWeekPlanGenerator(LlmProviderClient provider, ObjectMapper objectMapper) {
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public NextWeekPlanGeneration generate(NextWeekPlanInput input) {
		Objects.requireNonNull(input, "input must not be null");
		LlmStructuredResponse response;
		try {
			response = provider.requestStructured(new LlmStructuredRequest(
				LlmUsagePurpose.NEXT_WEEK_ADJUST,
				INSTRUCTIONS,
				input(input),
				schemaFormat(),
				requestPayload(input)
			));
		} catch (LlmProviderCallException exception) {
			throw new NextWeekPlanGenerationException("next week plan generation failed.", exception);
		}
		try {
			NextWeekPlan plan = readPlan(response.outputText());
			return new NextWeekPlanGeneration(
				plan,
				response.withResponseSummary("Generated next week plan: " + plan.tasks().size() + " tasks")
			);
		} catch (JsonProcessingException | IllegalArgumentException exception) {
			throw new NextWeekPlanGenerationException("next week plan output was invalid.", exception);
		}
	}

	private Map<String, Object> input(NextWeekPlanInput input) {
		return LlmPromptSanitizer.sanitizeMap(Map.of(
			"nextWeekNumber", input.weekNumber(),
			"nextWeekTitle", input.weekTitle(),
			"nextWeekSprintGoal", input.sprintGoal(),
			"previousWeekReport", input.reportText()
		));
	}

	private Map<String, Object> requestPayload(NextWeekPlanInput input) {
		return LlmPromptSanitizer.sanitizeMap(Map.of(
			"purpose", "NEXT_WEEK_ADJUST",
			"nextWeekNumber", input.weekNumber()
		));
	}

	private NextWeekPlan readPlan(String outputText) throws JsonProcessingException {
		JsonNode node = objectMapper.readTree(outputText);
		JsonNode tasksNode = node.get("tasks");
		if (tasksNode == null || !tasksNode.isArray() || tasksNode.isEmpty()) {
			throw new IllegalArgumentException("tasks must be a non-empty array.");
		}
		List<CurriculumTaskPlan> tasks = new ArrayList<>();
		for (JsonNode task : tasksNode) {
			tasks.add(new CurriculumTaskPlan(
				WeeklyTaskType.valueOf(requireText(task, "taskType")),
				requireText(task, "title"),
				requireText(task, "description"),
				task.path("required").asBoolean(true)
			));
		}
		return new NextWeekPlan(tasks, requireText(node, "retrospectivePrompt"));
	}

	private static String requireText(JsonNode node, String field) {
		String value = node.path(field).asText(null);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("required field '" + field + "' must not be missing or blank.");
		}
		return value;
	}

	private Map<String, Object> schemaFormat() {
		Map<String, Object> taskSchema = Map.of(
			"type", "object",
			"required", List.of("taskType", "title", "description", "required"),
			"additionalProperties", false,
			"properties", Map.of(
				"taskType", Map.of("type", "string", "enum", TASK_TYPES),
				"title", Map.of("type", "string"),
				"description", Map.of("type", "string"),
				"required", Map.of("type", "boolean")
			)
		);
		return Map.of(
			"type", "json_schema",
			"name", "next_week_plan",
			"schema", Map.of(
				"type", "object",
				"required", List.of("tasks", "retrospectivePrompt"),
				"additionalProperties", false,
				"properties", Map.of(
					"tasks", Map.of("type", "array", "items", taskSchema),
					"retrospectivePrompt", Map.of("type", "string")
				)
			)
		);
	}
}
