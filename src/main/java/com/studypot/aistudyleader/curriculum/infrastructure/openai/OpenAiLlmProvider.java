package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.llm.domain.LlmModelPricing;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.service.LlmCallFailure;
import com.studypot.aistudyleader.llm.service.LlmProviderCallException;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmStructuredRequest;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

class OpenAiLlmProvider implements LlmProviderClient {

	private final OpenAiResponsesTransport transport;
	private final ObjectMapper objectMapper;
	private final String defaultModel;
	private final OpenAiApiMode apiMode;
	private final OpenAiOutputTokenLimits outputTokenLimits;
	private final OpenAiPurposeModels purposeModels;
	private final OpenAiReasoningEfforts reasoningEfforts;

	OpenAiLlmProvider(OpenAiResponsesTransport transport, ObjectMapper objectMapper, String model) {
		this(transport, objectMapper, model, OpenAiApiMode.RESPONSES);
	}

	OpenAiLlmProvider(OpenAiResponsesTransport transport, ObjectMapper objectMapper, String model, OpenAiApiMode apiMode) {
		this(transport, objectMapper, model, apiMode, OpenAiOutputTokenLimits.defaults());
	}

	OpenAiLlmProvider(
		OpenAiResponsesTransport transport,
		ObjectMapper objectMapper,
		String model,
		OpenAiApiMode apiMode,
		OpenAiOutputTokenLimits outputTokenLimits
	) {
		this(transport, objectMapper, model, apiMode, outputTokenLimits, OpenAiPurposeModels.none());
	}

	OpenAiLlmProvider(
		OpenAiResponsesTransport transport,
		ObjectMapper objectMapper,
		String model,
		OpenAiApiMode apiMode,
		OpenAiOutputTokenLimits outputTokenLimits,
		OpenAiPurposeModels purposeModels
	) {
		this(transport, objectMapper, model, apiMode, outputTokenLimits, purposeModels, OpenAiReasoningEfforts.defaults());
	}

	OpenAiLlmProvider(
		OpenAiResponsesTransport transport,
		ObjectMapper objectMapper,
		String model,
		OpenAiApiMode apiMode,
		OpenAiOutputTokenLimits outputTokenLimits,
		OpenAiPurposeModels purposeModels,
		OpenAiReasoningEfforts reasoningEfforts
	) {
		this.transport = Objects.requireNonNull(transport, "transport must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
		if (model == null || model.isBlank()) {
			throw new IllegalArgumentException("model must not be blank");
		}
		this.defaultModel = model.strip();
		this.apiMode = Objects.requireNonNull(apiMode, "apiMode must not be null");
		this.outputTokenLimits = Objects.requireNonNull(outputTokenLimits, "outputTokenLimits must not be null");
		this.purposeModels = Objects.requireNonNull(purposeModels, "purposeModels must not be null");
		this.reasoningEfforts = Objects.requireNonNull(reasoningEfforts, "reasoningEfforts must not be null");
	}

	@Override
	public LlmStructuredResponse requestStructured(LlmStructuredRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		Instant startedAt = Instant.now();
		String requestModel = modelFor(request);
		try {
			OpenAiResponseRequest responseRequest = requestFor(request, requestModel);
			String responseBody = transport.createResponse(responseRequest);
			JsonNode response = objectMapper.readTree(responseBody);
			TokenUsage usage = tokenUsage(response);
			int inputTokens = usage.inputTokens();
			int outputTokens = usage.outputTokens();
			String outputText = outputText(response, request, requestModel, inputTokens, outputTokens, elapsedMillis(startedAt));
			return new LlmStructuredResponse(
				LlmProvider.OPENAI,
				requestModel,
				outputText,
				inputTokens,
				outputTokens,
				LlmModelPricing.costUsd(requestModel, inputTokens, outputTokens),
				elapsedMillis(startedAt),
				LlmUsageStatus.SUCCESS,
				null,
				providerRequestPayload(request, requestModel),
				"OpenAI structured response received."
			);
		} catch (LlmProviderCallException exception) {
			throw exception;
		} catch (JsonProcessingException exception) {
			throw failure(request, requestModel, "OPENAI_RESPONSE_INVALID_JSON", "OpenAI response could not be parsed.", exception, elapsedMillis(startedAt));
		} catch (RuntimeException exception) {
			throw failure(request, requestModel, "OPENAI_REQUEST_FAILED", "OpenAI request failed.", exception, elapsedMillis(startedAt));
		}
	}

	private OpenAiResponseRequest requestFor(LlmStructuredRequest request, String requestModel) {
		return switch (apiMode) {
			case RESPONSES -> responsesRequest(request, requestModel);
			case CHAT_COMPLETIONS -> chatCompletionsRequest(request, requestModel);
		};
	}

	private OpenAiResponseRequest responsesRequest(LlmStructuredRequest request, String requestModel) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", requestModel);
		body.put("instructions", request.instructions());
		body.put("input", inputJson(request, requestModel));
		body.put("text", Map.of("format", request.textFormat()));
		body.put("max_output_tokens", maxOutputTokens(request));
		return new OpenAiResponseRequest(body);
	}

	private OpenAiResponseRequest chatCompletionsRequest(LlmStructuredRequest request, String requestModel) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", requestModel);
		body.put("messages", List.of(
			Map.of("role", "developer", "content", request.instructions()),
			Map.of("role", "user", "content", inputJson(request, requestModel))
		));
		body.put("response_format", chatCompletionsResponseFormat(request.textFormat()));
		body.put("max_completion_tokens", maxOutputTokens(request));
		OpenAiReasoningEffort reasoningEffort = reasoningEffortFor(request, requestModel);
		if (reasoningEffort != null) {
			body.put("reasoning_effort", reasoningEffort.apiValue());
		}
		return new OpenAiResponseRequest("/chat/completions", body);
	}

	private Map<String, Object> chatCompletionsResponseFormat(Map<String, Object> textFormat) {
		if (textFormat.containsKey("json_schema") || !"json_schema".equals(textFormat.get("type"))) {
			return textFormat;
		}

		Map<String, Object> jsonSchema = new LinkedHashMap<>();
		copyIfPresent(textFormat, jsonSchema, "name");
		copyIfPresent(textFormat, jsonSchema, "schema");
		copyIfPresent(textFormat, jsonSchema, "strict");
		return Map.of(
			"type", "json_schema",
			"json_schema", jsonSchema
		);
	}

	private static void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
		Object value = source.get(key);
		if (value != null) {
			target.put(key, value);
		}
	}

	private String inputJson(LlmStructuredRequest request, String requestModel) {
		try {
			return objectMapper.writeValueAsString(request.input());
		} catch (JsonProcessingException exception) {
			throw failure(request, requestModel, "OPENAI_REQUEST_SERIALIZATION_FAILED", "OpenAI request input could not be serialized.", exception, null);
		}
	}

	private TokenUsage tokenUsage(JsonNode response) {
		JsonNode usage = response.path("usage");
		return switch (apiMode) {
			case RESPONSES -> new TokenUsage(
				usage.path("input_tokens").asInt(0),
				usage.path("output_tokens").asInt(0)
			);
			case CHAT_COMPLETIONS -> new TokenUsage(
				usage.path("prompt_tokens").asInt(0),
				usage.path("completion_tokens").asInt(0)
			);
		};
	}

	private String outputText(
		JsonNode response,
		LlmStructuredRequest request,
		String requestModel,
		int inputTokens,
		int outputTokens,
		Integer latencyMs
	) {
		if (apiMode == OpenAiApiMode.CHAT_COMPLETIONS) {
			return chatCompletionOutputText(response, request, requestModel, inputTokens, outputTokens, latencyMs);
		}
		return responsesOutputText(response, request, requestModel, inputTokens, outputTokens, latencyMs);
	}

	private String responsesOutputText(
		JsonNode response,
		LlmStructuredRequest request,
		String requestModel,
		int inputTokens,
		int outputTokens,
		Integer latencyMs
	) {
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
		throw failure(
			request,
			requestModel,
			"OPENAI_OUTPUT_TEXT_MISSING",
			"OpenAI response did not include output_text.",
			inputTokens,
			outputTokens,
			latencyMs
		);
	}

	private String chatCompletionOutputText(
		JsonNode response,
		LlmStructuredRequest request,
		String requestModel,
		int inputTokens,
		int outputTokens,
		Integer latencyMs
	) {
		for (JsonNode choice : response.path("choices")) {
			JsonNode content = choice.path("message").path("content");
			if (content.isTextual() && !content.asText().isBlank()) {
				return content.asText();
			}
			if (content.isArray()) {
				for (JsonNode part : content) {
					String text = part.path("text").asText();
					if (!text.isBlank()) {
						return text;
					}
				}
			}
		}
		throw failure(
			request,
			requestModel,
			"OPENAI_CHAT_COMPLETION_CONTENT_MISSING",
			"OpenAI chat completion response did not include message content.",
			inputTokens,
			outputTokens,
			latencyMs
		);
	}

	private LlmProviderCallException failure(
		LlmStructuredRequest request,
		String requestModel,
		String errorCode,
		String summary,
		Throwable cause,
		Integer latencyMs
	) {
		return new LlmProviderCallException(
			summary,
			cause,
			failureAudit(request, requestModel, errorCode, summary, 0, 0, latencyMs)
		);
	}

	private LlmProviderCallException failure(
		LlmStructuredRequest request,
		String requestModel,
		String errorCode,
		String summary,
		int inputTokens,
		int outputTokens,
		Integer latencyMs
	) {
		return new LlmProviderCallException(
			summary,
			failureAudit(request, requestModel, errorCode, summary, inputTokens, outputTokens, latencyMs)
		);
	}

	private LlmCallFailure failureAudit(
		LlmStructuredRequest request,
		String requestModel,
		String errorCode,
		String summary,
		int inputTokens,
		int outputTokens,
		Integer latencyMs
	) {
		return new LlmCallFailure(
			request.purpose(),
			LlmProvider.OPENAI,
			requestModel,
			inputTokens,
			outputTokens,
			LlmModelPricing.costUsd(requestModel, inputTokens, outputTokens),
			latencyMs,
			LlmUsageStatus.FAILED,
			errorCode,
			providerRequestPayload(request, requestModel),
			summary
		);
	}

	private int maxOutputTokens(LlmStructuredRequest request) {
		return outputTokenLimits.forPurpose(request.purpose());
	}

	private String modelFor(LlmStructuredRequest request) {
		// AI 팀장 채팅은 PREMIUM 플랜만 상위(per-purpose) 모델을 쓰고,
		// 그 외(FREE/미지정)는 기본 모델(저가)로 내려 비용을 통제한다. 다른 용도는 플랜과 무관.
		if (request.purpose() == LlmUsagePurpose.TEAM_LEAD_CHAT && !isPremiumPlan(request.userPlan())) {
			return defaultModel;
		}
		return purposeModels.modelFor(request.purpose(), defaultModel);
	}

	private static boolean isPremiumPlan(String plan) {
		return plan != null && "PREMIUM".equalsIgnoreCase(plan.strip());
	}

	private OpenAiReasoningEffort reasoningEffortFor(LlmStructuredRequest request, String requestModel) {
		OpenAiReasoningEffort reasoningEffort = reasoningEfforts.forPurpose(request.purpose());
		if (reasoningEffort == null || !supportsReasoningEffort(requestModel)) {
			return null;
		}
		return reasoningEffort;
	}

	private static boolean supportsReasoningEffort(String model) {
		String normalized = model.strip().toLowerCase(Locale.ROOT);
		return normalized.startsWith("gpt-5");
	}

	private Map<String, Object> providerRequestPayload(LlmStructuredRequest request, String requestModel) {
		Map<String, Object> result = new LinkedHashMap<>(request.requestPayload());
		result.put("apiMode", apiMode.name());
		result.put("outputBudget", maxOutputTokens(request));
		OpenAiReasoningEffort reasoningEffort = reasoningEffortFor(request, requestModel);
		if (apiMode == OpenAiApiMode.CHAT_COMPLETIONS && reasoningEffort != null) {
			result.put("reasoningEffort", reasoningEffort.apiValue());
		}
		return Map.copyOf(result);
	}

	private static int elapsedMillis(Instant startedAt) {
		long millis = Duration.between(startedAt, Instant.now()).toMillis();
		if (millis <= 0) {
			return 0;
		}
		return millis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) millis;
	}

	private record TokenUsage(int inputTokens, int outputTokens) {
	}
}
