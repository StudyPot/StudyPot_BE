package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.service.LlmCallFailure;
import com.studypot.aistudyleader.llm.service.LlmProviderCallException;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmStructuredRequest;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

class OpenAiLlmProvider implements LlmProviderClient {

	private final OpenAiResponsesTransport transport;
	private final ObjectMapper objectMapper;
	private final String model;

	OpenAiLlmProvider(OpenAiResponsesTransport transport, ObjectMapper objectMapper, String model) {
		this.transport = Objects.requireNonNull(transport, "transport must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
		if (model == null || model.isBlank()) {
			throw new IllegalArgumentException("model must not be blank");
		}
		this.model = model.strip();
	}

	@Override
	public LlmStructuredResponse requestStructured(LlmStructuredRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		Instant startedAt = Instant.now();
		try {
			OpenAiResponseRequest responseRequest = responseRequest(request);
			String responseBody = transport.createResponse(responseRequest);
			JsonNode response = objectMapper.readTree(responseBody);
			JsonNode usage = response.path("usage");
			int inputTokens = usage.path("input_tokens").asInt(0);
			int outputTokens = usage.path("output_tokens").asInt(0);
			String outputText = outputText(response, request, inputTokens, outputTokens, elapsedMillis(startedAt));
			return new LlmStructuredResponse(
				LlmProvider.OPENAI,
				model,
				outputText,
				inputTokens,
				outputTokens,
				BigDecimal.ZERO,
				elapsedMillis(startedAt),
				LlmUsageStatus.SUCCESS,
				null,
				request.requestPayload(),
				"OpenAI response output_text received."
			);
		} catch (LlmProviderCallException exception) {
			throw exception;
		} catch (JsonProcessingException exception) {
			throw failure(request, "OPENAI_RESPONSE_INVALID_JSON", "OpenAI response could not be parsed.", exception, elapsedMillis(startedAt));
		} catch (RuntimeException exception) {
			throw failure(request, "OPENAI_REQUEST_FAILED", "OpenAI request failed.", exception, elapsedMillis(startedAt));
		}
	}

	private OpenAiResponseRequest responseRequest(LlmStructuredRequest request) {
		return new OpenAiResponseRequest(Map.of(
			"model", model,
			"instructions", request.instructions(),
			"input", inputJson(request),
			"text", Map.of("format", request.textFormat())
		));
	}

	private String inputJson(LlmStructuredRequest request) {
		try {
			return objectMapper.writeValueAsString(request.input());
		} catch (JsonProcessingException exception) {
			throw failure(request, "OPENAI_REQUEST_SERIALIZATION_FAILED", "OpenAI request input could not be serialized.", exception, null);
		}
	}

	private String outputText(JsonNode response, LlmStructuredRequest request, int inputTokens, int outputTokens, Integer latencyMs) {
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
			"OPENAI_OUTPUT_TEXT_MISSING",
			"OpenAI response did not include output_text.",
			inputTokens,
			outputTokens,
			latencyMs
		);
	}

	private LlmProviderCallException failure(
		LlmStructuredRequest request,
		String errorCode,
		String summary,
		Throwable cause,
		Integer latencyMs
	) {
		return new LlmProviderCallException(
			summary,
			cause,
			failureAudit(request, errorCode, summary, 0, 0, latencyMs)
		);
	}

	private LlmProviderCallException failure(
		LlmStructuredRequest request,
		String errorCode,
		String summary,
		int inputTokens,
		int outputTokens,
		Integer latencyMs
	) {
		return new LlmProviderCallException(
			summary,
			failureAudit(request, errorCode, summary, inputTokens, outputTokens, latencyMs)
		);
	}

	private LlmCallFailure failureAudit(
		LlmStructuredRequest request,
		String errorCode,
		String summary,
		int inputTokens,
		int outputTokens,
		Integer latencyMs
	) {
		return new LlmCallFailure(
			request.purpose(),
			LlmProvider.OPENAI,
			model,
			inputTokens,
			outputTokens,
			BigDecimal.ZERO,
			latencyMs,
			LlmUsageStatus.FAILED,
			errorCode,
			request.requestPayload(),
			summary
		);
	}

	private static int elapsedMillis(Instant startedAt) {
		long millis = Duration.between(startedAt, Instant.now()).toMillis();
		if (millis <= 0) {
			return 0;
		}
		return millis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) millis;
	}
}
