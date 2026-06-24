package com.studypot.aistudyleader.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmPromptSanitizer;
import com.studypot.aistudyleader.llm.service.LlmStructuredRequest;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * LLM 으로 질문 게시판 글을 사용자 지시에 맞춰 재작성한다. 안전·범위 규칙은 유지하고 형식/표현만 다듬는다.
 */
class ProviderBackedAiConversationQuestionRefiner implements AiConversationQuestionRefiner {

	private static final String INSTRUCTIONS = """
		너는 그룹 '질문' 게시판에 올릴 학습 질문 글을 다듬는 편집자다.
		입력: originalTitle(원래 질문 제목), originalSummary(질문과 답변 요약), userInstruction(작성자가 원하는 방식).
		userInstruction 을 최대한 반영해 한국어로 게시판 글을 다시 작성하라.
		- title: 간결한 한 줄 제목.
		- content: 질문 맥락과 핵심을 담은 본문(마크다운 허용). 답변까지 포함해도 좋다.
		스터디 학습 범위를 벗어나지 말고, 비밀키·자격증명류 값은 포함하지 마라.
		제공된 JSON 스키마(title, content)에 맞는 JSON 만 반환하라.
		""";

	private final LlmProviderClient provider;
	private final ObjectMapper objectMapper;
	private final LlmUsageRecorder usageRecorder;
	private final Supplier<UUID> idGenerator;
	private final Clock clock;

	ProviderBackedAiConversationQuestionRefiner(
		LlmProviderClient provider,
		ObjectMapper objectMapper,
		LlmUsageRecorder usageRecorder,
		Supplier<UUID> idGenerator,
		Clock clock
	) {
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
		this.usageRecorder = Objects.requireNonNull(usageRecorder, "usageRecorder must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	@Override
	public RefinedQuestionPost refine(
		UUID authenticatedUserId,
		UUID groupId,
		String originalTitle,
		String originalSummary,
		String instruction
	) {
		LlmStructuredResponse response = provider.requestStructured(new LlmStructuredRequest(
			LlmUsagePurpose.TEAM_LEAD_CHAT,
			INSTRUCTIONS,
			input(originalTitle, originalSummary, instruction),
			schemaFormat(),
			Map.of("purpose", "TEAM_LEAD_CHAT", "kind", "question_share_refine")
		));
		usageRecorder.record(response.toUsage(
			idGenerator.get(),
			authenticatedUserId,
			groupId,
			LlmUsagePurpose.TEAM_LEAD_CHAT,
			clock.instant()
		));
		JsonNode node = parseJsonLenient(response.outputText());
		String title = node == null ? null : textOrNull(node.get("title"));
		String content = node == null ? null : textOrNull(node.get("content"));
		if (title == null || content == null) {
			throw new IllegalStateException("question refine output was invalid.");
		}
		return new RefinedQuestionPost(title, content);
	}

	private Map<String, Object> input(String originalTitle, String originalSummary, String instruction) {
		return LlmPromptSanitizer.sanitizeMap(Map.of(
			"originalTitle", originalTitle == null ? "" : originalTitle,
			"originalSummary", originalSummary == null ? "" : originalSummary,
			"userInstruction", instruction == null ? "" : instruction
		));
	}

	private Map<String, Object> schemaFormat() {
		return Map.of(
			"type", "json_schema",
			"name", "question_share_refine",
			"schema", Map.of(
				"type", "object",
				"required", List.of("title", "content"),
				"properties", Map.of(
					"title", Map.of("type", "string"),
					"content", Map.of("type", "string")
				)
			)
		);
	}

	private JsonNode parseJsonLenient(String outputText) {
		if (outputText == null || outputText.isBlank()) {
			return null;
		}
		String candidate = outputText.strip();
		if (candidate.startsWith("```")) {
			candidate = candidate.replaceFirst("^```[a-zA-Z0-9]*\\s*", "").replaceFirst("\\s*```$", "").strip();
		}
		int start = candidate.indexOf('{');
		int end = candidate.lastIndexOf('}');
		if (start >= 0 && end > start) {
			candidate = candidate.substring(start, end + 1);
		}
		try {
			JsonNode node = objectMapper.readTree(candidate);
			return node != null && node.isObject() ? node : null;
		} catch (JsonProcessingException exception) {
			return null;
		}
	}

	private static String textOrNull(JsonNode node) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return null;
		}
		String value = node.asText();
		return value == null || value.isBlank() ? null : value.strip();
	}
}
