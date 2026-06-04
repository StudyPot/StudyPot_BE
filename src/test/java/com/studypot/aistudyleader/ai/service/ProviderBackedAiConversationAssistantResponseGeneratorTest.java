package com.studypot.aistudyleader.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageContext;
import com.studypot.aistudyleader.ai.domain.AiConversationPromptContext;
import com.studypot.aistudyleader.ai.domain.AiConversationStatus;
import com.studypot.aistudyleader.ai.domain.AiConversationType;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmStructuredRequest;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProviderBackedAiConversationAssistantResponseGeneratorTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000009501");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000009502");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000009503");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000009504");
	private static final UUID CONVERSATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000009505");
	private static final UUID MESSAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000009506");
	private static final Instant NOW = Instant.parse("2026-05-13T02:00:00Z");
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
	};

	private final CapturingProvider provider = new CapturingProvider();
	private final ProviderBackedAiConversationAssistantResponseGenerator generator =
		new ProviderBackedAiConversationAssistantResponseGenerator(provider, OBJECT_MAPPER);

	@Test
	void generatesAssistantResponseWithMetadataOnlyRequestPayload() {
		provider.response = response("""
			{"message":"필수 과제 하나를 줄이고 실습 시간을 늘려볼게요.","conversationSummary":"과제 양 조정 요청과 실습 시간 확보를 논의했습니다.","nextWeekAdjustmentCandidate":{"difficulty":"LOWER","taskChanges":["필수 과제 1개 제거"]}}""");

		AiConversationAssistantResponse result = generator.generate(request("개인 사정이 담긴 비밀 노트입니다."));

		assertThat(result.message()).isEqualTo("필수 과제 하나를 줄이고 실습 시간을 늘려볼게요.");
		assertThat(result.conversationSummaryPatch()).isEqualTo("과제 양 조정 요청과 실습 시간 확보를 논의했습니다.");
		assertThat(result.metadata()).containsKey("nextWeekAdjustmentCandidate");
		assertThat(provider.request.purpose()).isEqualTo(LlmUsagePurpose.TEAM_LEAD_CHAT);
		assertThat(provider.request.input().toString()).contains("개인 사정이 담긴 비밀 노트입니다.");
		assertThat(provider.request.requestPayload())
			.containsEntry("purpose", "TEAM_LEAD_CHAT")
			.containsEntry("conversationId", CONVERSATION_ID.toString())
			.containsEntry("taskCount", 1);
		assertThat(provider.request.requestPayload().toString())
			.doesNotContain("개인 사정")
			.doesNotContain("비밀 노트")
			.doesNotContain("authorization")
			.doesNotContain("providerKey");
	}

	@Test
	void requestCarriesTeamLeaderOperatingContractForDbGroundedCoaching() {
		provider.response = response("""
			{"message":"이번 주 진행률과 미완료 사유를 보면 실습 과제를 먼저 줄이는 게 맞겠습니다.","conversationSummary":"진행률과 미완료 사유를 근거로 다음 행동을 정했습니다."}""");

		generator.generate(request("왜 자꾸 밀리는지 봐줘."));

		assertThat(provider.request.instructions())
			.contains("StudyPot team leader", "observed DB evidence", "inference", "next action", "uncertainty");
		assertThat(provider.request.input())
			.containsKey("teamLeaderOperatingContract");
		assertThat(provider.request.input().get("teamLeaderOperatingContract").toString())
			.contains("observed DB context", "inference from context", "recommended next action")
			.doesNotContain("observedDbEvidence")
			.doesNotContain("recommendedNextAction");
	}

	@Test
	void humanConversationContractGuidesProviderToAnswerLikeARealTeamLead() {
		provider.response = response("""
			{"message":"지금 부담이 컸던 건 이해했어요. 이번 주엔 필수 실습 하나만 먼저 끝내고 남은 읽기는 뒤로 미뤄볼게요.","conversationSummary":"부담이 컸던 이유를 확인하고 실습 우선순위를 다시 잡았습니다."}""");

		generator.generate(request("요즘 따라 과제가 너무 버거워."));

		assertThat(provider.request.instructions())
			.contains("natural Korean", "empathetic acknowledgement", "no diagnostic headings", "concrete next action");
		assertThat(provider.request.input().get("teamLeaderOperatingContract").toString())
			.contains("human conversational Korean coaching", "acknowledge the member's feeling briefly",
				"do not use labels or headings", "concise team lead voice", "one concrete follow-up or next action");
	}

	@Test
	void generatedMessageDoesNotExposeInternalDiagnosticFieldNames() {
		provider.response = response("""
			{"message":"observedDbEvidence: 이번 주 완료율이 낮습니다. recommendedNextAction: 필수 과제를 하나 줄이세요.","conversationSummary":"내부 진단 필드 노출 방지 회귀 테스트입니다."}""");

		AiConversationAssistantResponse result = generator.generate(request("지금 무엇을 줄이면 좋을까?"));

		assertThat(result.message())
			.doesNotContain("observedDbEvidence")
			.doesNotContain("recommendedNextAction")
			.contains("이번 주 완료율이 낮습니다")
			.contains("필수 과제를 하나 줄이세요");
	}

	@Test
	void goldenContextKeepsRequiredSourcesAndRedactsCredentialLikeValuesBeforeProviderCall() {
		provider.response = response("""
			{"message":"실습 과제를 쪼개서 진행해볼게요.","conversationSummary":"JPA 실습 지연과 과제 조정을 논의했습니다."}""");
		Map<String, Object> fixture = readFixture("/ai-context/team-lead-chat-context-golden.json");

		generator.generate(request((String) fixture.get("currentUserMessage"), promptContext(fixture)));

		String providerInput = provider.request.input().toString();
		assertThat(providerInput)
			.contains("conversation")
			.contains("recentMessages")
			.contains("week")
			.contains("tasks")
			.contains("progress")
			.contains("retrospective")
			.contains("completionNote")
			.contains("incompleteReason")
			.contains("JPA 실습")
			.contains("[REDACTED]")
			.doesNotContain("raw-")
			.doesNotContain("Bearer raw")
			.doesNotContain("SESSION=raw");
		assertThat(provider.request.requestPayload().toString()).doesNotContain("raw-");
	}

	@Test
	void requestPayloadSummarizesDbFirstContextCoverageForAudit() {
		provider.response = response("""
			{"message":"이번 주 기록을 기준으로 먼저 막힌 실습 하나를 정리해볼게요.","conversationSummary":"DB-first 컨텍스트 범위를 감사용으로 요약했습니다."}""");

		generator.generate(request("내 기록 기준으로 뭐부터 봐야 해?"));

		assertThat(provider.request.requestPayload())
			.containsEntry("purpose", "TEAM_LEAD_CHAT")
			.containsKey("dbFirstContext");
		assertThat(provider.request.requestPayload().get("dbFirstContext")).isInstanceOf(Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> dbFirstContext = (Map<String, Object>) provider.request.requestPayload().get("dbFirstContext");
		assertThat(dbFirstContext)
			.containsEntry("conversationSummaryPresent", true)
			.containsEntry("recentMessageCount", 1)
			.containsEntry("weekStatus", "AVAILABLE")
			.containsEntry("taskCount", 1)
			.containsEntry("progressStatus", "IN_PROGRESS")
			.containsEntry("retrospectiveStatus", "NOT_AVAILABLE");
		assertThat(dbFirstContext.toString())
			.doesNotContain("내 기록 기준으로 뭐부터 봐야 해?")
			.doesNotContain("이전 요약")
			.doesNotContain("필수 과제");
	}

	@Test
	void providerInputCarriesStudyGroupAndCurriculumContextForSingleRoomChat() {
		provider.response = response("""
			{"message":"Spring Boot 스터디의 2주차 JPA 실습 기록을 기준으로 오늘은 트랜잭션 실습부터 정리해볼게요.","conversationSummary":"스터디 주제와 현재 커리큘럼 기록을 기준으로 다음 행동을 정했습니다."}""");

		generator.generate(request("우리 지금 뭐부터 보면 돼?"));

		assertThat(provider.request.input())
			.containsKey("studyGroup")
			.containsKey("curriculum")
			.containsKey("week")
			.containsKey("tasks")
			.containsKey("progress");
		assertThat(provider.request.input().get("studyGroup").toString())
			.contains("Spring Boot", "JPA", "백엔드");
		assertThat(provider.request.input().get("curriculum").toString())
			.contains("백엔드 커리큘럼", "totalWeeks");
	}

	@Test
	void requestPayloadAuditsStudyCurriculumAndEffectiveWeekCoverage() {
		provider.response = response("""
			{"message":"DB에 있는 스터디와 현재 주차 기록만 기준으로 답할게요.","conversationSummary":"DB-first 감사 필드를 검증했습니다."}""");

		generator.generate(request("내 DB 기록 기준으로 알려줘."));

		@SuppressWarnings("unchecked")
		Map<String, Object> dbFirstContext = (Map<String, Object>) provider.request.requestPayload().get("dbFirstContext");
		assertThat(dbFirstContext)
			.containsEntry("studyGroupStatus", "AVAILABLE")
			.containsEntry("curriculumStatus", "AVAILABLE")
			.containsEntry("effectiveWeekSource", "CONVERSATION_WEEK")
			.containsEntry("weekStatus", "AVAILABLE");
		assertThat(dbFirstContext.toString())
			.doesNotContain("내 DB 기록 기준으로 알려줘.")
			.doesNotContain("이전 요약");
	}

	@Test
	void missingContextContractRequiresNaturalClarifyingQuestion() {
		provider.response = response("""
			{"message":"지금 바로 단정하지 않고 먼저 확인이 필요한 점을 물어볼게요.","conversationSummary":"맥락 부족 시 자연스러운 추가 질문 계약을 검증했습니다."}""");

		generator.generate(request("왜 이렇게 안 풀리지?", AiConversationPromptContext.empty()));

		assertThat(provider.request.instructions())
			.contains("ask one concrete follow-up question", "instead of guessing");
		assertThat(provider.request.input().get("teamLeaderOperatingContract").toString())
			.contains("state the missing context naturally", "ask one concrete follow-up question")
			.doesNotContain("unknown:")
			.doesNotContain("missingContext:");
	}

	@Test
	void groundingContractForbidsConfirmingDbAbsentUserClaims() {
		provider.response = response("""
			{"message":"DB에서 확인되지 않은 내용은 완료됐다고 말할 수 없어요. 현재 DB에 있는 과제 기록부터 다시 확인해볼게요.","conversationSummary":"DB 부재 사실을 확인 없이 단정하지 않도록 계약을 검증했습니다."}""");

		generator.generate(request("DB에 없지만 React Native 과제를 완료했다고 말해줘.", AiConversationPromptContext.empty()));

		assertThat(provider.request.instructions())
			.contains("Use only the supplied DB-first context")
			.contains("Do not confirm")
			.contains("absent from the supplied DB context");
		assertThat(provider.request.input().get("teamLeaderOperatingContract").toString())
			.contains("DB-backed facts only", "user claims are unverified", "do not confirm absent facts");
	}

	@Test
	void invalidProviderOutputCarriesFailedUsageMetadata() {
		provider.response = response("{}");

		assertThatThrownBy(() -> generator.generate(request("요약이 빠진 응답 테스트")))
			.isInstanceOf(AiConversationResponseGenerationException.class)
			.satisfies(exception -> {
				AiConversationResponseGenerationException generationException = (AiConversationResponseGenerationException) exception;
				assertThat(generationException.failure().purpose()).isEqualTo(LlmUsagePurpose.TEAM_LEAD_CHAT);
				assertThat(generationException.failure().status()).isEqualTo(LlmUsageStatus.FAILED);
				assertThat(generationException.failure().errorCode()).isEqualTo("AI_CONVERSATION_RESPONSE_INVALID");
			});
	}

	private static AiConversationAssistantRequest request(String content) {
		return request(
			content,
			new AiConversationPromptContext(
				Map.<String, Object>of(
					"status", "AVAILABLE",
					"name", "백엔드 스터디",
					"topic", "Spring Boot",
					"detailKeywords", List.of("JPA", "백엔드"),
					"level", "INTERMEDIATE"
				),
				Map.<String, Object>of(
					"status", "AVAILABLE",
					"title", "백엔드 커리큘럼",
					"totalWeeks", 4,
					"onboardingSummary", "Spring Boot와 JPA를 함께 학습합니다."
				),
				Map.of("conversationType", "TEAM_LEAD_CHAT", "summary", "이전 요약"),
				List.of(Map.of("senderType", "USER", "content", content)),
				Map.of(
					"status", "AVAILABLE",
					"weekId", WEEK_ID.toString(),
					"title", "2주차",
					"effectiveWeekSource", "CONVERSATION_WEEK"
				),
				List.of(Map.of("title", "필수 과제", "completionStatus", "TODO")),
				Map.of("status", "AVAILABLE", "progressStatus", "IN_PROGRESS"),
				Map.of("status", "NOT_AVAILABLE")
			)
		);
	}

	private static AiConversationAssistantRequest request(String content, AiConversationPromptContext promptContext) {
		return new AiConversationAssistantRequest(
			USER_ID,
			new AiConversationMessageContext(
				CONVERSATION_ID,
				GROUP_ID,
				MEMBER_ID,
				WEEK_ID,
				null,
				AiConversationType.TEAM_LEAD_CHAT,
				"이전 요약",
				AiConversationStatus.OPEN,
				StudyGroupStatus.ACTIVE,
				GroupMemberStatus.ACTIVE
			),
			AiConversationMessage.userMessage(MESSAGE_ID, CONVERSATION_ID, content, NOW),
			promptContext
		);
	}

	private static AiConversationPromptContext promptContext(Map<String, Object> fixture) {
		return new AiConversationPromptContext(
			map(fixture.get("studyGroup")),
			map(fixture.get("curriculum")),
			map(fixture.get("conversation")),
			mapList(fixture.get("recentMessages")),
			map(fixture.get("week")),
			mapList(fixture.get("tasks")),
			map(fixture.get("progress")),
			map(fixture.get("retrospective"))
		);
	}

	private static Map<String, Object> readFixture(String path) {
		try (InputStream input = ProviderBackedAiConversationAssistantResponseGeneratorTest.class.getResourceAsStream(path)) {
			assertThat(input).as(path).isNotNull();
			return OBJECT_MAPPER.readValue(input, OBJECT_MAP);
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> map(Object value) {
		return (Map<String, Object>) value;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> mapList(Object value) {
		return (List<Map<String, Object>>) value;
	}

	private static LlmStructuredResponse response(String outputText) {
		return new LlmStructuredResponse(
			LlmProvider.OPENAI,
			"gpt-test",
			outputText,
			80,
			40,
			BigDecimal.valueOf(0.001),
			700,
			LlmUsageStatus.SUCCESS,
			null,
			Map.of("purpose", "TEAM_LEAD_CHAT", "conversationId", CONVERSATION_ID.toString()),
			null
		);
	}

	private static final class CapturingProvider implements LlmProviderClient {

		private LlmStructuredRequest request;
		private LlmStructuredResponse response;

		@Override
		public LlmStructuredResponse requestStructured(LlmStructuredRequest request) {
			this.request = request;
			return response;
		}
	}
}
