package com.studypot.aistudyleader.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageContext;
import com.studypot.aistudyleader.ai.domain.AiConversationPromptContext;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.service.LlmProviderCallException;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmPromptSanitizer;
import com.studypot.aistudyleader.llm.service.LlmStructuredRequest;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

class ProviderBackedAiConversationAssistantResponseGenerator implements AiConversationAssistantResponseGenerator {

	private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
	};
	private static final String INSTRUCTIONS = """
		You are the StudyPot team leader for the authenticated member, not a generic assistant.
		Return only JSON matching the provided AI conversation response schema.
		Write the message as an operator who checked the supplied DB-first context.
		Ground the answer in observed DB evidence, explain the inference from that evidence, state uncertainty when context is missing, and end with a concrete next action.
		Use only the supplied DB-first context and the authenticated member's conversation.
		Do not infer private details about other members.
		Do not include secrets, OAuth data, provider keys, cookies, or credential-like values.
		""";

	private final LlmProviderClient provider;
	private final ObjectMapper objectMapper;

	ProviderBackedAiConversationAssistantResponseGenerator(LlmProviderClient provider, ObjectMapper objectMapper) {
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public AiConversationAssistantResponse generate(AiConversationAssistantRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		LlmStructuredResponse response;
		try {
			response = provider.requestStructured(new LlmStructuredRequest(
				LlmUsagePurpose.TEAM_LEAD_CHAT,
				INSTRUCTIONS,
				input(request),
				schemaFormat(),
				requestPayload(request)
			));
		} catch (LlmProviderCallException exception) {
			throw new AiConversationResponseGenerationException(
				"AI conversation response generation failed.",
				exception,
				exception.failure()
			);
		}
		try {
			GeneratedAiConversationResponse generated = readResponse(response.outputText());
			return new AiConversationAssistantResponse(
				generated.message(),
				generated.conversationSummary(),
				generated.metadata(),
				response.withResponseSummary("Generated AI conversation response for conversation " + request.messageContext().conversationId())
			);
		} catch (JsonProcessingException | IllegalArgumentException exception) {
			throw new AiConversationResponseGenerationException(
				"AI conversation response output was invalid.",
				exception,
				response.toFailure(
					LlmUsagePurpose.TEAM_LEAD_CHAT,
					"AI_CONVERSATION_RESPONSE_INVALID",
					"Generated AI conversation response did not match the required shape."
				)
			);
		}
	}

	private Map<String, Object> input(AiConversationAssistantRequest request) {
		AiConversationPromptContext context = request.promptContext();
		return LlmPromptSanitizer.sanitizeMap(Map.of(
			"conversation", context.conversation(),
			"recentMessages", context.messages(),
			"currentUserMessage", request.userMessage().content(),
			"week", context.week(),
			"tasks", context.tasks(),
			"progress", context.progress(),
			"retrospective", context.retrospective(),
			"teamLeaderOperatingContract", teamLeaderOperatingContract()
		));
	}

	private Map<String, Object> teamLeaderOperatingContract() {
		return Map.of(
			"role", "StudyPot team leader",
			"messageMustInclude", List.of(
				"observedDbEvidence",
				"inferenceFromContext",
				"recommendedNextAction"
			),
			"missingContextRule", "If progress, tasks, week, or retrospective context is missing, say what is unknown instead of guessing.",
			"style", "coach the member with concise study-operations language"
		);
	}

	private Map<String, Object> requestPayload(AiConversationAssistantRequest request) {
		AiConversationMessageContext context = request.messageContext();
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("purpose", "TEAM_LEAD_CHAT");
		payload.put("conversationId", context.conversationId().toString());
		payload.put("conversationType", context.conversationType().name());
		payload.put("groupId", context.groupId().toString());
		payload.put("memberId", context.memberId().toString());
		putUuid(payload, "weekId", context.curriculumWeekId());
		putUuid(payload, "retrospectiveId", context.retrospectiveId());
		payload.put("recentMessageCount", request.promptContext().messages().size());
		payload.put("taskCount", request.promptContext().tasks().size());
		payload.put("retrospectiveStatus", statusOf(request.promptContext().retrospective()));
		return LlmPromptSanitizer.sanitizeMap(payload);
	}

	private GeneratedAiConversationResponse readResponse(String outputText) throws JsonProcessingException {
		JsonNode node = objectMapper.readTree(outputText);
		String message = requiredText(node.get("message"), "message");
		String conversationSummary = requiredText(node.get("conversationSummary"), "conversationSummary");
		JsonNode adjustmentNode = node.get("nextWeekAdjustmentCandidate");
		Map<String, Object> metadata = new LinkedHashMap<>();
		if (adjustmentNode != null && adjustmentNode.isObject() && !adjustmentNode.isEmpty()) {
			metadata.put("nextWeekAdjustmentCandidate", objectMapper.convertValue(adjustmentNode, OBJECT_MAP));
		}
		return new GeneratedAiConversationResponse(message, conversationSummary, metadata);
	}

	private Map<String, Object> schemaFormat() {
		return Map.of(
			"type", "json_schema",
			"name", "ai_conversation_response",
			"schema", Map.of(
				"type", "object",
				"required", List.of("message", "conversationSummary"),
				"properties", Map.of(
					"message", Map.of("type", "string"),
					"conversationSummary", Map.of("type", "string"),
					"nextWeekAdjustmentCandidate", Map.of(
						"type", "object",
						"properties", Map.of(
							"difficulty", Map.of("type", "string"),
							"taskChanges", Map.of("type", "array", "items", Map.of("type", "string")),
							"supportMaterials", Map.of("type", "array", "items", Map.of("type", "string"))
						)
					)
				)
			)
		);
	}

	private static String requiredText(JsonNode node, String fieldName) {
		String value = node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank.");
		}
		return value.strip();
	}

	private static String statusOf(Map<String, Object> source) {
		if (source == null) {
			return "UNKNOWN";
		}
		Object status = source.get("status");
		return status == null ? "UNKNOWN" : status.toString();
	}

	private static void putUuid(Map<String, Object> target, String key, UUID value) {
		if (value != null) {
			target.put(key, value.toString());
		}
	}

	private record GeneratedAiConversationResponse(
		String message,
		String conversationSummary,
		Map<String, Object> metadata
	) {
	}
}
