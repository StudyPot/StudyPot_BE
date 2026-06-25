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

class ProviderBackedStudyCompletionReportGenerator implements StudyCompletionReportGenerator {

	private static final String INSTRUCTIONS = """
		당신은 스터디 그룹의 AI 팀장입니다. 스터디 전체 기간(모든 주차)을 마무리하며
		팀 전체를 위한 '수료 리포트'를 따뜻하고 격려하는 어조의 한국어로 작성하세요.
		- memberRetrospectives(전체 회고)가 있으면 그 내용을 중심으로, 팀이 무엇을 배우고 어떻게 성장했는지 요약합니다.
		- 회고가 부족하면 memberTaskProgress(각 멤버의 전체 TODO 완료/전체 개수)로 전체 진행과 성과를 요약합니다.
		- 구성: 전체 여정 요약 / 잘한 점·성과 / 아쉬웠던 점 / 앞으로의 학습 제안. 마지막엔 완주 축하 한마디.
		- 멤버 개개인의 사적인 정보나 비난은 넣지 마세요. 팀 관점으로 작성합니다.
		- 리포트는 그 자체로 완결돼야 합니다. "원하면 다음 글에서는 …해줄게", "다음 편에서…" 처럼 다음 글을 예고/약속하는 문장은 절대 넣지 마세요.
		- 비밀키, OAuth, 자격증명류 값은 절대 포함하지 마세요.
		- 반드시 제공된 JSON 스키마(title, body)에 맞는 JSON 만 반환하세요. body 는 마크다운 단락으로 작성합니다.
		""";

	private final LlmProviderClient provider;
	private final ObjectMapper objectMapper;

	ProviderBackedStudyCompletionReportGenerator(LlmProviderClient provider, ObjectMapper objectMapper) {
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public WeeklyReportGeneration generate(StudyCompletionReportData data) {
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
			throw new WeeklyReportGenerationException("study completion report generation failed.", exception);
		}
		try {
			WeeklyReportContent content = readContent(response.outputText());
			return new WeeklyReportGeneration(
				content,
				response.withResponseSummary("Generated study completion report: " + content.title())
			);
		} catch (JsonProcessingException | IllegalArgumentException exception) {
			throw new WeeklyReportGenerationException("study completion report output was invalid.", exception);
		}
	}

	private Map<String, Object> input(StudyCompletionReportData data) {
		List<Map<String, Object>> members = new ArrayList<>();
		for (MemberRetrospectiveSummary retro : data.memberRetrospectives()) {
			members.add(Map.of("member", retro.memberName(), "retrospective", retro.summary()));
		}
		List<Map<String, Object>> taskProgress = new ArrayList<>();
		for (MemberTaskProgress progress : data.memberTaskProgress()) {
			taskProgress.add(Map.of(
				"member", progress.memberName(),
				"doneCount", progress.doneCount(),
				"totalCount", progress.totalCount()
			));
		}
		return LlmPromptSanitizer.sanitizeMap(Map.of(
			"studyName", data.studyName(),
			"totalWeeks", data.totalWeeks(),
			"memberRetrospectives", members,
			"memberTaskProgress", taskProgress
		));
	}

	private Map<String, Object> requestPayload(StudyCompletionReportData data) {
		return LlmPromptSanitizer.sanitizeMap(Map.of(
			"purpose", "STUDY_COMPLETION_REPORT",
			"groupId", data.groupId().toString(),
			"totalWeeks", data.totalWeeks(),
			"memberCount", data.memberTaskProgress().size()
		));
	}

	private WeeklyReportContent readContent(String outputText) throws JsonProcessingException {
		JsonNode node = objectMapper.readTree(outputText);
		return new WeeklyReportContent(node.path("title").asText(null), node.path("body").asText(null));
	}

	private Map<String, Object> schemaFormat() {
		return Map.of(
			"type", "json_schema",
			"name", "study_completion_report",
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
