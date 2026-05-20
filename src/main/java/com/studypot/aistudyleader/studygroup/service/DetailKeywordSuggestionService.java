package com.studypot.aistudyleader.studygroup.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.service.LlmProviderCallException;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import com.studypot.aistudyleader.llm.service.LlmStructuredRequest;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class DetailKeywordSuggestionService {

	private static final String INSTRUCTIONS = """
		Suggest detailed study keywords for a new study group.
		Return only JSON matching the schema with the top-level keywords array. Do not persist candidates.
		Prefer concrete, selectable Korean study keywords.
		""";

	private final LlmProviderClient provider;
	private final ObjectMapper objectMapper;
	private final LlmUsageRecorder usageRecorder;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;

	public DetailKeywordSuggestionService(
		LlmProviderClient provider,
		ObjectMapper objectMapper,
		LlmUsageRecorder usageRecorder,
		Clock clock,
		Supplier<UUID> idGenerator
	) {
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
		this.usageRecorder = Objects.requireNonNull(usageRecorder, "usageRecorder must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
	}

	public DetailKeywordSuggestions suggest(SuggestDetailKeywordsCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		Instant now = clock.instant();
		LlmStructuredResponse response;
		try {
			response = provider.requestStructured(new LlmStructuredRequest(
				LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST,
				INSTRUCTIONS,
				Map.of(
					"topic", command.topic(),
					"hintKeywords", command.hintKeywords(),
					"maxCandidates", command.maxCandidates()
				),
				schemaFormat(),
				Map.of(
					"purpose", "DETAIL_KEYWORD_SUGGEST",
					"topic", command.topic(),
					"hintKeywordCount", command.hintKeywords().size(),
					"maxCandidates", command.maxCandidates()
				)
			));
		} catch (LlmProviderCallException exception) {
			usageRecorder.record(exception.failure().toUsage(idGenerator.get(), command.authenticatedUserId(), null, now));
			throw new StudyGroupServiceUnavailableException("detail keyword suggestions could not be generated.", exception);
		}
		try {
			DetailKeywordSuggestions suggestions = readSuggestions(response.outputText());
			LlmStructuredResponse summarized = response.withResponseSummary(
				"Generated detail keyword suggestions: " + suggestions.keywords().size()
			);
			usageRecorder.record(summarized.toUsage(
				idGenerator.get(),
				command.authenticatedUserId(),
				null,
				LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST,
				now
			));
			return suggestions;
		} catch (JsonProcessingException | IllegalArgumentException exception) {
			usageRecorder.record(response.toFailure(
					LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST,
					"DETAIL_KEYWORD_RESPONSE_INVALID",
					"Generated detail keyword suggestions did not match the required shape."
				)
				.toUsage(idGenerator.get(), command.authenticatedUserId(), null, now));
			throw new StudyGroupServiceUnavailableException("detail keyword suggestions could not be generated.", exception);
		}
	}

	private DetailKeywordSuggestions readSuggestions(String outputText) throws JsonProcessingException {
		JsonNode node = objectMapper.readTree(outputText);
		JsonNode keywords = node.path("keywords");
		if (!keywords.isArray()) {
			throw new IllegalArgumentException("generated keywords must be an array");
		}
		List<String> generated = objectMapper.convertValue(
			keywords,
			new TypeReference<List<String>>() {
			}
		);
		return new DetailKeywordSuggestions(generated);
	}

	private Map<String, Object> schemaFormat() {
		return Map.of(
			"type", "json_schema",
			"name", "detail_keyword_suggestions",
			"strict", true,
			"schema", Map.of(
				"type", "object",
				"required", List.of("keywords"),
				"additionalProperties", false,
				"properties", Map.of(
					"keywords", Map.of(
						"type", "array",
						"minItems", 1,
						"maxItems", 10,
						"items", Map.of("type", "string")
					)
				)
			)
		);
	}
}
