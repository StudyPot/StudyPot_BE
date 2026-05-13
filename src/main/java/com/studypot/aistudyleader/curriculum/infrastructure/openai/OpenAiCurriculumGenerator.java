package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGeneration;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGenerationRequest;
import com.studypot.aistudyleader.curriculum.domain.CurriculumTaskPlan;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeekPlan;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import com.studypot.aistudyleader.curriculum.service.CurriculumGenerationException;
import com.studypot.aistudyleader.curriculum.service.CurriculumGenerator;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class OpenAiCurriculumGenerator implements CurriculumGenerator {

	private static final String INSTRUCTIONS = """
		You generate a study curriculum from submitted onboarding summaries.
		Return JSON that matches the provided schema. Use only the supplied context.
		Do not include retrospective feedback, notifications, member progress, or chat behavior.
		""";
	private final OpenAiResponsesTransport transport;
	private final ObjectMapper objectMapper;
	private final String model;

	OpenAiCurriculumGenerator(OpenAiResponsesTransport transport, ObjectMapper objectMapper, String model) {
		this.transport = Objects.requireNonNull(transport, "transport must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
		if (model == null || model.isBlank()) {
			throw new IllegalArgumentException("model must not be blank");
		}
		this.model = model.strip();
	}

	@Override
	public CurriculumGeneration generate(CurriculumGenerationRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		Instant startedAt = Instant.now();
		OpenAiResponseRequest responseRequest = responseRequest(request);
		try {
			String responseBody = transport.createResponse(responseRequest);
			JsonNode response = objectMapper.readTree(responseBody);
			GeneratedCurriculum generated = readGeneratedCurriculum(outputText(response));
			JsonNode usage = response.path("usage");
			return new CurriculumGeneration(
				generated.title(),
				generated.toWeekPlans(),
				INSTRUCTIONS,
				LlmProvider.OPENAI,
				model,
				usage.path("input_tokens").asInt(0),
				usage.path("output_tokens").asInt(0),
				BigDecimal.ZERO,
				elapsedMillis(startedAt),
				LlmUsageStatus.SUCCESS,
				null,
				redactedRequestPayload(request),
				"Generated curriculum title: " + generated.title() + ", weeks: " + generated.weeks().size()
			);
		} catch (CurriculumGenerationException exception) {
			throw exception;
		} catch (RuntimeException | JsonProcessingException exception) {
			throw new CurriculumGenerationException("curriculum generation failed.", exception);
		}
	}

	private OpenAiResponseRequest responseRequest(CurriculumGenerationRequest request) {
		return new OpenAiResponseRequest(Map.of(
			"model", model,
			"instructions", INSTRUCTIONS,
			"input", inputJson(request),
			"text", Map.of("format", schemaFormat())
		));
	}

	private String inputJson(CurriculumGenerationRequest request) {
		try {
			return objectMapper.writeValueAsString(Map.of(
				"group", Map.of(
					"id", request.group().groupId().toString(),
					"name", request.group().groupName(),
					"topic", request.group().topic(),
					"detailKeywords", request.group().detailKeywords(),
					"startsAt", request.group().startsAt().toString(),
					"endsAt", request.group().endsAt().toString()
				),
				"onboardingSummary", request.onboardingSummary(),
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
			));
		} catch (JsonProcessingException exception) {
			throw new CurriculumGenerationException("curriculum generation input could not be serialized.", exception);
		}
	}

	private Map<String, Object> redactedRequestPayload(CurriculumGenerationRequest request) {
		return Map.of(
			"purpose", "CURRICULUM_GENERATE",
			"model", model,
			"groupId", request.group().groupId().toString(),
			"submittedResponseCount", request.submittedResponses().size()
		);
	}

	private Map<String, Object> schemaFormat() {
		Map<String, Object> taskSchema = Map.of(
			"type", "object",
			"required", List.of("taskType", "title", "required"),
			"properties", Map.of(
				"taskType", Map.of("type", "string", "enum", List.of("READING", "PRACTICE", "ASSIGNMENT", "PROJECT", "CUSTOM")),
				"title", Map.of("type", "string"),
				"description", Map.of("type", "string"),
				"required", Map.of("type", "boolean")
			)
		);
		Map<String, Object> weekSchema = Map.of(
			"type", "object",
			"required", List.of("weekNumber", "title", "sprintGoal", "learningGoals", "resources", "tasks"),
			"properties", Map.of(
				"weekNumber", Map.of("type", "integer"),
				"title", Map.of("type", "string"),
				"sprintGoal", Map.of("type", "string"),
				"learningGoals", Map.of("type", "array", "items", Map.of("type", "string")),
				"resources", Map.of("type", "array", "items", Map.of(
					"type", "object",
					"properties", Map.of("title", Map.of("type", "string"), "url", Map.of("type", "string"))
				)),
				"tasks", Map.of("type", "array", "items", taskSchema)
			)
		);
		return Map.of(
			"type", "json_schema",
			"name", "curriculum_generation",
			"schema", Map.of(
				"type", "object",
				"required", List.of("title", "totalWeeks", "weeks"),
				"properties", Map.of(
					"title", Map.of("type", "string"),
					"totalWeeks", Map.of("type", "integer"),
					"weeks", Map.of("type", "array", "items", weekSchema)
				)
			)
		);
	}

	private String outputText(JsonNode response) {
		for (JsonNode output : response.path("output")) {
			for (JsonNode content : output.path("content")) {
				if ("output_text".equals(content.path("type").asText())) {
					String text = content.path("text").asText();
					if (!text.isBlank()) {
						return text;
					}
				}
			}
		}
		throw new CurriculumGenerationException("OpenAI response did not include output_text.");
	}

	private GeneratedCurriculum readGeneratedCurriculum(String value) throws JsonProcessingException {
		JsonNode node = objectMapper.readTree(value);
		String title = node.path("title").asText();
		List<GeneratedWeek> weeks = objectMapper.convertValue(node.path("weeks"), new TypeReference<List<GeneratedWeek>>() {
		});
		return new GeneratedCurriculum(title, weeks);
	}

	private static int elapsedMillis(Instant startedAt) {
		long millis = Duration.between(startedAt, Instant.now()).toMillis();
		if (millis <= 0) {
			return 0;
		}
		return millis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) millis;
	}

	private record GeneratedCurriculum(String title, List<GeneratedWeek> weeks) {

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
				throw new CurriculumGenerationException("generated taskType must not be blank.");
			}
			try {
				return WeeklyTaskType.valueOf(taskType.strip());
			} catch (IllegalArgumentException exception) {
				throw new CurriculumGenerationException("generated taskType is unsupported: " + taskType, exception);
			}
		}
	}
}
