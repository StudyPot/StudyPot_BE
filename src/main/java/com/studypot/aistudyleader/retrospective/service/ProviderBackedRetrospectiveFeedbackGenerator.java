package com.studypot.aistudyleader.retrospective.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.service.LlmProviderCallException;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmPromptSanitizer;
import com.studypot.aistudyleader.llm.service.LlmStructuredRequest;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import com.studypot.aistudyleader.retrospective.domain.Retrospective;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveFeedbackResult;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class ProviderBackedRetrospectiveFeedbackGenerator implements RetrospectiveFeedbackGenerator {

	private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
	};
	private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
	};
	private static final String INSTRUCTIONS = """
		Generate weekly retrospective feedback for the authenticated member.
		Return only JSON matching the provided retrospective feedback schema.
		Use only the supplied DB-first context. Do not infer private details about other members.
		Do not include secrets, OAuth data, provider keys, or raw credential-like values.
		""";

	private final LlmProviderClient provider;
	private final ObjectMapper objectMapper;

	ProviderBackedRetrospectiveFeedbackGenerator(LlmProviderClient provider, ObjectMapper objectMapper) {
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public RetrospectiveFeedbackGeneration generate(Retrospective retrospective) {
		Objects.requireNonNull(retrospective, "retrospective must not be null");
		LlmStructuredResponse response;
		try {
			response = provider.requestStructured(new LlmStructuredRequest(
				LlmUsagePurpose.RETROSPECTIVE_FEEDBACK,
				INSTRUCTIONS,
				input(retrospective),
				schemaFormat(),
				requestPayload(retrospective)
			));
		} catch (LlmProviderCallException exception) {
			throw new RetrospectiveFeedbackGenerationException(
				"retrospective feedback generation failed.",
				exception,
				exception.failure()
			);
		}
		try {
			RetrospectiveFeedbackResult result = readFeedbackResult(response.outputText());
			return new RetrospectiveFeedbackGeneration(
				result,
				response.withResponseSummary("Generated retrospective feedback summary: " + result.aiFeedback().get("summary"))
			);
		} catch (JsonProcessingException | IllegalArgumentException exception) {
			throw new RetrospectiveFeedbackGenerationException(
				"retrospective feedback output was invalid.",
				exception,
				response.toFailure(
					LlmUsagePurpose.RETROSPECTIVE_FEEDBACK,
					"RETROSPECTIVE_RESPONSE_INVALID",
					"Generated retrospective feedback did not match the required shape."
				)
			);
		}
	}

	private Map<String, Object> input(Retrospective retrospective) {
		return LlmPromptSanitizer.sanitizeMap(Map.of(
			"retrospectiveId", retrospective.id().toString(),
			"weekId", retrospective.curriculumWeekId().toString(),
			"memberId", retrospective.memberId().toString(),
			"context", retrospective.inputSummary()
		));
	}

	private Map<String, Object> requestPayload(Retrospective retrospective) {
		Map<String, Object> context = retrospective.inputSummary();
		return LlmPromptSanitizer.sanitizeMap(Map.of(
			"purpose", "RETROSPECTIVE_FEEDBACK",
			"retrospectiveId", retrospective.id().toString(),
			"weekId", retrospective.curriculumWeekId().toString(),
			"memberId", retrospective.memberId().toString(),
			"taskCount", countList(context.get("tasks")),
			"ruleViolationCount", countList(context.get("ruleViolations")),
			"priorRetrospectiveCount", countList(context.get("priorRetrospectives")),
			"conversationSummaryStatus", conversationSummaryStatus(context.get("conversationSummary"))
		));
	}

	private RetrospectiveFeedbackResult readFeedbackResult(String outputText) throws JsonProcessingException {
		JsonNode node = objectMapper.readTree(outputText);
		Map<String, Object> nextWeekAdjustment = nextWeekAdjustment(node);
		return RetrospectiveFeedbackResult.of(
			node.path("summary").asText(null),
			stringList(node.get("strengths")),
			stringList(node.get("risks")),
			stringList(node.get("actionItems")),
			nextWeekAdjustment
		);
	}

	private Map<String, Object> nextWeekAdjustment(JsonNode node) {
		JsonNode nextWeekAdjustment = node.get("nextWeekAdjustment");
		if (nextWeekAdjustment == null || !nextWeekAdjustment.isObject() || nextWeekAdjustment.isEmpty()) {
			throw new IllegalArgumentException("nextWeekAdjustment must be a non-empty object.");
		}
		return objectMapper.convertValue(nextWeekAdjustment, OBJECT_MAP);
	}

	private List<String> stringList(JsonNode node) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return List.of();
		}
		return objectMapper.convertValue(node, STRING_LIST);
	}

	private Map<String, Object> schemaFormat() {
		Map<String, Object> stringArray = Map.of("type", "array", "items", Map.of("type", "string"));
		Map<String, Object> memberNote = Map.of(
			"type", "object",
			"required", List.of("memberId", "note"),
			"properties", Map.of(
				"memberId", Map.of("type", "string"),
				"note", Map.of("type", "string")
			)
		);
		return Map.of(
			"type", "json_schema",
			"name", "retrospective_feedback",
			"schema", Map.of(
				"type", "object",
				"required", List.of("summary", "strengths", "risks", "actionItems", "nextWeekAdjustment"),
				"properties", Map.of(
					"summary", Map.of("type", "string"),
					"strengths", stringArray,
					"risks", stringArray,
					"actionItems", stringArray,
					"nextWeekAdjustment", Map.of(
						"type", "object",
						"minProperties", 1,
						"properties", Map.of(
							"difficulty", Map.of("type", "string"),
							"taskChanges", stringArray,
							"supportMaterials", stringArray,
							"memberNotes", Map.of("type", "array", "items", memberNote)
						)
					)
				)
			)
		);
	}

	private static int countList(Object value) {
		return value instanceof List<?> list ? list.size() : 0;
	}

	private static String conversationSummaryStatus(Object value) {
		if (value instanceof Map<?, ?> map) {
			Object status = map.get("status");
			return status == null ? "UNKNOWN" : status.toString();
		}
		return "UNKNOWN";
	}
}
