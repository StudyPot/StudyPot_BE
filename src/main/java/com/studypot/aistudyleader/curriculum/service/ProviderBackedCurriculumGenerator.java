package com.studypot.aistudyleader.curriculum.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGeneration;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGenerationRequest;
import com.studypot.aistudyleader.curriculum.domain.CurriculumSprintWindow;
import com.studypot.aistudyleader.curriculum.domain.CurriculumTaskPlan;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeekPlan;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.service.LlmProviderCallException;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmStructuredRequest;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProviderBackedCurriculumGenerator implements CurriculumGenerator {

	private static final String INSTRUCTIONS = """
		You generate a study curriculum from submitted onboarding summaries.
		Use the supplied fixed one-week sprint plan as the required week structure.
		Set totalWeeks to expectedWeekCount and return exactly one week per sprint window.
		Number weeks sequentially from 1 to expectedWeekCount.
		Return JSON that matches the provided schema. Use only the supplied context.
		For each week, also produce retrospectivePrompt: 한국어로 그 주차의 task/목표를 돌아볼 수 있는
		회고 유도 질문 2~4개를 한 문자열로 작성한다(줄바꿈으로 구분). 이는 회고를 작성할 때 보여줄
		'질문 프롬프트'이며 AI 피드백이 아니다.
		Do not include retrospective feedback, notifications, member progress, or chat behavior.
		""";

	private final LlmProviderClient provider;
	private final ObjectMapper objectMapper;

	public ProviderBackedCurriculumGenerator(LlmProviderClient provider, ObjectMapper objectMapper) {
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public CurriculumGeneration generate(CurriculumGenerationRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		LlmStructuredResponse response;
		try {
			response = provider.requestStructured(new LlmStructuredRequest(
				LlmUsagePurpose.CURRICULUM_GENERATE,
				INSTRUCTIONS,
				input(request),
				schemaFormat(),
				requestPayload(request)
			));
		} catch (LlmProviderCallException exception) {
			throw new CurriculumGenerationException("curriculum generation failed.", exception, exception.failure());
		}
		try {
			GeneratedCurriculum generated = readGeneratedCurriculum(response.outputText(), request.expectedWeekCount());
			String responseSummary = "Generated curriculum title: " + generated.title() + ", weeks: " + generated.weeks().size();
			LlmStructuredResponse summarized = response.withResponseSummary(responseSummary);
			return new CurriculumGeneration(
				generated.title(),
				generated.toWeekPlans(),
				INSTRUCTIONS,
				summarized.provider(),
				summarized.model(),
				summarized.inputTokens(),
				summarized.outputTokens(),
				summarized.totalCostUsd(),
				summarized.latencyMs(),
				summarized.status(),
				summarized.errorCode(),
				summarized.requestPayload(),
				summarized.responseSummary()
			);
		} catch (IllegalArgumentException | JsonProcessingException exception) {
			throw new CurriculumGenerationException(
				"curriculum generation output was invalid.",
				exception,
				response.toFailure(
					LlmUsagePurpose.CURRICULUM_GENERATE,
					"CURRICULUM_RESPONSE_INVALID",
					"Generated curriculum response did not match the required shape."
				)
			);
		}
	}

	private Map<String, Object> input(CurriculumGenerationRequest request) {
		return Map.of(
			"group", Map.of(
				"id", request.group().groupId().toString(),
				"name", request.group().groupName(),
				"topic", request.group().topic(),
				"detailKeywords", request.group().detailKeywords(),
				"startsAt", request.group().startsAt().toString(),
				"endsAt", request.group().endsAt().toString()
			),
			"onboardingSummary", request.onboardingSummary(),
			"sprintPlan", Map.of(
				"unit", "P1W",
				"expectedWeekCount", request.expectedWeekCount(),
				"windows", request.sprintWindows().stream()
					.map(this::sprintWindowInput)
					.toList()
			),
			"submittedResponses", request.submittedResponses().stream()
				.map(response -> Map.of(
					"id", response.id().toString(),
					"memberId", response.memberId().toString(),
					"keywordSkillLevels", response.keywordSkillLevels(),
					"taskPreferences", response.taskPreferences(),
					"additionalNote", response.additionalNote() == null ? "" : response.additionalNote(),
					"availabilitySlots", response.availabilitySlots(),
					"submittedAt", response.submittedAt().toString()
				))
				.toList()
		);
	}

	private Map<String, Object> sprintWindowInput(CurriculumSprintWindow window) {
		return Map.of(
			"weekNumber", window.weekNumber(),
			"startsAt", window.startsAt().toString(),
			"endsAt", window.endsAt().toString()
		);
	}

	private Map<String, Object> requestPayload(CurriculumGenerationRequest request) {
		return Map.of(
			"purpose", "CURRICULUM_GENERATE",
			"groupId", request.group().groupId().toString(),
			"submittedResponseCount", request.submittedResponses().size(),
			"sprintUnit", "P1W",
			"expectedWeekCount", request.expectedWeekCount()
		);
	}

	private Map<String, Object> schemaFormat() {
		Map<String, Object> resourceSchema = Map.of(
			"type", "object",
			"required", List.of("title", "url"),
			"additionalProperties", false,
			"properties", Map.of(
				"title", Map.of("type", "string"),
				"url", Map.of("type", "string")
			)
		);
		Map<String, Object> taskSchema = Map.of(
			"type", "object",
			"required", List.of("taskType", "title", "description", "required"),
			"additionalProperties", false,
			"properties", Map.of(
				"taskType", Map.of("type", "string", "enum", List.of("READING", "PRACTICE", "ASSIGNMENT", "PROJECT", "CUSTOM")),
				"title", Map.of("type", "string"),
				"description", Map.of("type", "string"),
				"required", Map.of("type", "boolean")
			)
		);
		Map<String, Object> weekSchema = Map.of(
			"type", "object",
			"required", List.of("weekNumber", "title", "sprintGoal", "retrospectivePrompt", "learningGoals", "resources", "tasks"),
			"additionalProperties", false,
			"properties", Map.of(
				"weekNumber", Map.of("type", "integer"),
				"title", Map.of("type", "string"),
				"sprintGoal", Map.of("type", "string"),
				"retrospectivePrompt", Map.of("type", "string"),
				"learningGoals", Map.of("type", "array", "items", Map.of("type", "string")),
				"resources", Map.of("type", "array", "items", resourceSchema),
				"tasks", Map.of("type", "array", "items", taskSchema)
			)
		);
		return Map.of(
			"type", "json_schema",
			"name", "curriculum_generation",
			"strict", true,
			"schema", Map.of(
				"type", "object",
				"required", List.of("title", "totalWeeks", "weeks"),
				"additionalProperties", false,
				"properties", Map.of(
					"title", Map.of("type", "string"),
					"totalWeeks", Map.of("type", "integer"),
					"weeks", Map.of("type", "array", "items", weekSchema)
				)
			)
		);
	}

	private GeneratedCurriculum readGeneratedCurriculum(String value, int expectedWeekCount) throws JsonProcessingException {
		JsonNode node = objectMapper.readTree(value);
		String title = node.path("title").asText();
		int totalWeeks = node.path("totalWeeks").asInt(-1);
		List<GeneratedWeek> weeks = objectMapper.convertValue(node.path("weeks"), new TypeReference<List<GeneratedWeek>>() {
		});
		if (weeks == null || weeks.isEmpty()) {
			throw new IllegalArgumentException("generated weeks must not be empty");
		}
		if (totalWeeks != weeks.size()) {
			throw new IllegalArgumentException("generated totalWeeks must match weeks size");
		}
		if (totalWeeks != expectedWeekCount) {
			throw new IllegalArgumentException("generated totalWeeks must match expected week count");
		}
		for (int i = 0; i < weeks.size(); i++) {
			if (weeks.get(i).weekNumber() != i + 1) {
				throw new IllegalArgumentException("generated weekNumber must be sequential");
			}
		}
		return new GeneratedCurriculum(title, weeks);
	}

	private record GeneratedCurriculum(String title, List<GeneratedWeek> weeks) {

		private GeneratedCurriculum {
			if (title == null || title.isBlank()) {
				throw new IllegalArgumentException("generated title must not be blank");
			}
			weeks = List.copyOf(Objects.requireNonNull(weeks, "generated weeks must not be null"));
		}

		List<CurriculumWeekPlan> toWeekPlans() {
			return weeks.stream()
				.map(GeneratedWeek::toWeekPlan)
				.toList();
		}
	}

	private record GeneratedWeek(
		int weekNumber,
		String title,
		String sprintGoal,
		String retrospectivePrompt,
		List<String> learningGoals,
		List<Map<String, String>> resources,
		List<GeneratedTask> tasks
	) {

		CurriculumWeekPlan toWeekPlan() {
			List<GeneratedTask> generatedTasks = Objects.requireNonNull(tasks, "generated tasks must not be null");
			return new CurriculumWeekPlan(
				weekNumber,
				title,
				sprintGoal,
				retrospectivePrompt,
				learningGoals == null ? List.of() : learningGoals,
				resources == null ? List.of() : resources,
				generatedTasks.stream().map(GeneratedTask::toTaskPlan).toList()
			);
		}
	}

	private record GeneratedTask(
		String taskType,
		String title,
		String description,
		boolean required
	) {

		CurriculumTaskPlan toTaskPlan() {
			return new CurriculumTaskPlan(parsedTaskType(), title, description, required);
		}

		private WeeklyTaskType parsedTaskType() {
			if (taskType == null || taskType.isBlank()) {
				throw new IllegalArgumentException("generated taskType must not be blank.");
			}
			try {
				return WeeklyTaskType.valueOf(taskType.strip());
			} catch (IllegalArgumentException exception) {
				throw new IllegalArgumentException("generated taskType is unsupported: " + taskType, exception);
			}
		}
	}
}
