package com.studypot.aistudyleader.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

class ProviderBackedWeeklyReportGenerator implements WeeklyReportGenerator {

	private static final String INSTRUCTIONS = """
		당신은 스터디 그룹의 AI 팀장입니다. 한 주차 동안 모든 멤버가 작성한 회고를 종합해
		팀 전체를 위한 '주차 학습 리포트'를 한국어로 작성하세요.
		- 멤버 개개인의 사적인 정보나 비난은 넣지 마세요. 팀 관점의 요약/성과/개선점/다음 주 제안 위주로 작성합니다.
		- 비밀키, OAuth, 자격증명류 값은 절대 포함하지 마세요.
		- 반드시 제공된 JSON 스키마(title, body)에 맞는 JSON 만 반환하세요. body 는 마크다운 단락으로 작성합니다.
		""";

	private final LlmProviderClient provider;
	private final ObjectMapper objectMapper;

	ProviderBackedWeeklyReportGenerator(LlmProviderClient provider, ObjectMapper objectMapper) {
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public WeeklyReportGeneration generate(WeeklyReportData data) {
		Objects.requireNonNull(data, "data must not be null");
		LlmStructuredResponse response;
		try {
			response = provider.requestStructured(new LlmStructuredRequest(
				LlmUsagePurpose.WEEKLY_REPORT,
				INSTRUCTIONS,
				input(data),
				schemaFormat(),
				requestPayload(data)
			));
		} catch (LlmProviderCallException exception) {
			throw new WeeklyReportGenerationException("weekly report generation failed.", exception);
		}
		try {
			WeeklyReportContent content = readContent(response.outputText());
			return new WeeklyReportGeneration(
				content,
				response.withResponseSummary("Generated weekly report: " + content.title())
			);
		} catch (JsonProcessingException | IllegalArgumentException exception) {
			throw new WeeklyReportGenerationException("weekly report output was invalid.", exception);
		}
	}

	private Map<String, Object> input(WeeklyReportData data) {
		List<Map<String, Object>> members = new ArrayList<>();
		for (MemberRetrospectiveSummary retro : data.memberRetrospectives()) {
			members.add(Map.of("member", retro.memberName(), "retrospective", retro.summary()));
		}
		return LlmPromptSanitizer.sanitizeMap(Map.of(
			"weekNumber", data.weekNumber(),
			"weekTitle", data.weekTitle(),
			"memberRetrospectives", members
		));
	}

	private Map<String, Object> requestPayload(WeeklyReportData data) {
		return LlmPromptSanitizer.sanitizeMap(Map.of(
			"purpose", "WEEKLY_REPORT",
			"groupId", data.groupId().toString(),
			"weekId", data.weekId().toString(),
			"weekNumber", data.weekNumber(),
			"memberCount", data.memberRetrospectives().size()
		));
	}

	private WeeklyReportContent readContent(String outputText) throws JsonProcessingException {
		JsonNode node = objectMapper.readTree(outputText);
		return new WeeklyReportContent(node.path("title").asText(null), node.path("body").asText(null));
	}

	private Map<String, Object> schemaFormat() {
		return Map.of(
			"type", "json_schema",
			"name", "weekly_report",
			"schema", Map.of(
				"type", "object",
				"required", List.of("title", "body"),
				"properties", Map.of(
					"title", Map.of("type", "string"),
					"body", Map.of("type", "string")
				)
			)
		);
	}
}
