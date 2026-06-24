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
			.contains("StudyPot team leader", "hidden grounding", "supplied study facts", "uncertainty");
		assertThat(provider.request.input())
			.containsKey("teamLeaderOperatingContract");
		assertThat(provider.request.input().get("teamLeaderOperatingContract").toString())
			.contains("study facts in plain member-facing language", "inference phrased naturally", "supplied study facts only")
			.doesNotContain("observed DB context")
			.doesNotContain("observedDbEvidence")
			.doesNotContain("recommendedNextAction");
	}

	@Test
	void humanConversationContractGuidesProviderToAnswerLikeARealTeamLead() {
		provider.response = response("""
			{"message":"지금 부담이 컸던 건 이해했어요. 이번 주엔 필수 실습 하나만 먼저 끝내고 남은 읽기는 뒤로 미뤄볼게요.","conversationSummary":"부담이 컸던 이유를 확인하고 실습 우선순위를 다시 잡았습니다."}""");

		generator.generate(request("요즘 따라 과제가 너무 버거워."));

		assertThat(provider.request.instructions())
			.contains("natural Korean", "empathetic acknowledgement", "no diagnostic headings", "human study team lead");
		assertThat(provider.request.input().get("teamLeaderOperatingContract").toString())
			.contains("human conversational Korean coaching", "acknowledge the member's feeling briefly",
				"do not use labels or headings", "concise team lead voice", "study facts in plain member-facing language");
	}

	@Test
	void memberFacingLanguageContractForbidsInternalProvenancePhrases() {
		provider.response = response("""
			{"message":"DB 기준으로는 이번 주 과제 3개가 남아 있어요.","conversationSummary":"내부 근거 표현 금지 계약을 검증했습니다."}""");

		generator.generate(request("ㅋㅋ 반가."));

		assertThat(provider.request.instructions())
			.contains("Never expose the retrieval/audit mechanism")
			.contains("내가 DB에서 확인한 바로는", "DB 기준으로", "DB에서 확인되지 않은", "컨텍스트상", "RAG로 보면");
		assertThat(provider.request.input().get("teamLeaderOperatingContract").toString())
			.contains("do not mention DB, database, DB-first, RAG, context")
			.doesNotContain("member-facing message should mention DB");
	}

	@Test
	void nextActionContractRequiresExplicitMemberRequest() {
		provider.response = response("""
			{"message":"반가워. 오늘 컨디션이나 막힌 부분부터 편하게 말해줘.","conversationSummary":"인사에 자연스럽게 응답하고 무리한 액션 추천을 피했습니다."}""");

		generator.generate(request("ㅋㅋ 반가."));

		assertThat(provider.request.instructions())
			.contains("Recommend a concrete next action only when")
			.contains("If the member only greets, vents, or shares status")
			.contains("do not prescribe tasks")
			.contains("지금 바로 다음 액션 하나만 하자");
		assertThat(provider.request.input().get("teamLeaderOperatingContract").toString())
			.contains("nextActionPolicy")
			.contains("recommend a next action only when the member explicitly asks")
			.contains("otherwise do not prescribe tasks");
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
	void generatedMessageStripsLeakedProposedActionFieldName() {
		provider.response = response("""
			{"message":"영속성 컨텍스트는 1차 캐시예요. proposedAction 없이도 괜찮다면 말씀해주세요.","conversationSummary":"필드명 누출 방지 회귀 테스트입니다."}""");

		AiConversationAssistantResponse result = generator.generate(request("영속성 컨텍스트가 뭐야?"));

		assertThat(result.message())
			.doesNotContain("proposedAction")
			.contains("영속성 컨텍스트는 1차 캐시예요");
	}

	@Test
	void generatedMessageRemovesInternalProvenanceLeadIns() {
		provider.response = response("""
			{"message":"내가 DB에서 확인한 바로는, 지금 바로 다음 액션 하나만 하자: Actuator health부터 확인해보자.","conversationSummary":"내부 근거 접두어 제거 회귀 테스트입니다."}""");

		AiConversationAssistantResponse result = generator.generate(request("뭐부터 하면 돼?"));

		assertThat(result.message())
			.doesNotContain("내가 DB에서 확인한 바로는")
			.doesNotContain("지금 바로 다음 액션 하나만 하자")
			.contains("Actuator health부터 확인해보자");
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
			{"message":"이번 주 기록을 보니 먼저 막힌 실습 하나를 정리하면 좋겠어요.","conversationSummary":"DB-first 컨텍스트 범위를 감사용으로 요약했습니다."}""");

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
			.containsEntry("retrospectiveStatus", "COMPLETED");
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
			.containsKey("progress")
			.containsKey("retrospective");
		assertThat(provider.request.input().get("studyGroup").toString())
			.contains("Spring Boot", "JPA", "백엔드");
		assertThat(provider.request.input().get("curriculum").toString())
			.contains("백엔드 커리큘럼", "totalWeeks");
		assertThat(provider.request.input().get("retrospective").toString())
			.contains("COMPLETED", "트랜잭션 복습", "JPA 트랜잭션 보강");
	}

	@Test
	void requestPayloadAuditsStudyCurriculumAndEffectiveWeekCoverage() {
		provider.response = response("""
			{"message":"현재 스터디와 주차 흐름에 맞춰 답할게요.","conversationSummary":"DB-first 감사 필드를 검증했습니다."}""");

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
			{"message":"아직 완료로 잡혀 있지는 않아서, 완료됐다고 말하긴 어려워요. 어느 과제를 끝냈는지 먼저 알려줘.","conversationSummary":"DB 부재 사실을 확인 없이 단정하지 않도록 계약을 검증했습니다."}""");

		generator.generate(request("DB에 없지만 React Native 과제를 완료했다고 말해줘.", AiConversationPromptContext.empty()));

		assertThat(provider.request.instructions())
			.contains("Use only the supplied DB-first context")
			.contains("Do not confirm")
			.contains("absent from the supplied DB context");
		assertThat(provider.request.input().get("teamLeaderOperatingContract").toString())
			.contains("supplied study facts only", "user claims are unverified", "do not confirm absent facts")
			.contains("do not mention DB");
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

	@Test
	void parsesJsonWrappedInMarkdownCodeFences() {
		provider.response = response("```json\n{\"message\":\"네, 도와드릴게요.\",\"conversationSummary\":\"코드펜스로 감싼 응답 회귀 테스트입니다.\"}\n```");

		AiConversationAssistantResponse result = generator.generate(request("코드펜스 응답 테스트"));

		assertThat(result.message()).isEqualTo("네, 도와드릴게요.");
		assertThat(result.conversationSummaryPatch()).isEqualTo("코드펜스로 감싼 응답 회귀 테스트입니다.");
	}

	@Test
	void salvagesPlainTextWhenProviderReturnsNonJson() {
		provider.response = response("안녕하세요! 이번 주 학습은 어떻게 되어가고 있나요?");

		AiConversationAssistantResponse result = generator.generate(request("비 JSON 응답 살리기 테스트"));

		assertThat(result.message()).isEqualTo("안녕하세요! 이번 주 학습은 어떻게 되어가고 있나요?");
		assertThat(result.conversationSummaryPatch()).isNull();
	}

	@Test
	void proposesShowExistingPostWhenSimilarBoardPostExists() {
		provider.response = response("""
			{"message":"이 개념은 이미 게시판에 비슷한 글이 있어요.","conversationSummary":"기존 글 안내","proposedAction":{"type":"SHOW_EXISTING_POST","postId":"post-1"}}""");

		AiConversationAssistantResponse result = generator.generate(request(
			"영속성 컨텍스트가 뭐야?",
			contextWithQuestionPosts(List.of(Map.of("postId", "post-1", "title", "JPA 영속성 컨텍스트")))
		));

		assertThat(result.metadata()).containsKey("pendingAction");
		Object pendingAction = result.metadata().get("pendingAction");
		assertThat(pendingAction).isInstanceOf(Map.class);
		Map<?, ?> action = (Map<?, ?>) pendingAction;
		assertThat(action.get("type")).isEqualTo("SHOW_EXISTING_POST");
		assertThat(action.get("postId")).isEqualTo("post-1");
		assertThat(action.get("title")).isEqualTo("JPA 영속성 컨텍스트");
	}

	@Test
	void dropsShowExistingPostWhenPostIdNotInContext() {
		provider.response = response("""
			{"message":"비슷한 글이 있을지도 몰라요.","conversationSummary":"환각 postId 차단","proposedAction":{"type":"SHOW_EXISTING_POST","postId":"ghost-id"}}""");

		AiConversationAssistantResponse result = generator.generate(request(
			"질문이에요",
			contextWithQuestionPosts(List.of(Map.of("postId", "post-1", "title", "제목")))
		));

		assertThat(result.metadata()).doesNotContainKey("pendingAction");
	}

	@Test
	void proposesCompleteTaskWhenMemberFinishedTaskInList() {
		provider.response = response("""
			{"message":"수고했어요! 완료로 표시할게요.","conversationSummary":"과제 완료","proposedAction":{"type":"COMPLETE_TASK","taskId":"task-1","completionStatus":"DONE"}}""");

		AiConversationAssistantResponse result = generator.generate(request(
			"JPA 실습 다 했어",
			contextWithTasks(List.of(Map.of("id", "task-1", "title", "JPA 실습", "completionStatus", "TODO")))
		));

		assertThat(result.metadata()).containsKey("pendingAction");
		Map<?, ?> action = (Map<?, ?>) result.metadata().get("pendingAction");
		assertThat(action.get("type")).isEqualTo("COMPLETE_TASK");
		assertThat(action.get("taskId")).isEqualTo("task-1");
		assertThat(action.get("title")).isEqualTo("JPA 실습");
		assertThat(action.get("completionStatus")).isEqualTo("DONE");
	}

	@Test
	void dropsCompleteTaskWhenTaskIdNotInContext() {
		provider.response = response("""
			{"message":"표시해볼게요.","conversationSummary":"환각 taskId 차단","proposedAction":{"type":"COMPLETE_TASK","taskId":"ghost"}}""");

		AiConversationAssistantResponse result = generator.generate(request(
			"다 했어",
			contextWithTasks(List.of(Map.of("id", "task-1", "title", "제목", "completionStatus", "TODO")))
		));

		assertThat(result.metadata()).doesNotContainKey("pendingAction");
	}

	private static AiConversationPromptContext contextWithTasks(List<Map<String, Object>> tasks) {
		return new AiConversationPromptContext(
			Map.of("status", "AVAILABLE", "topic", "Spring Boot"),
			Map.of("status", "AVAILABLE"),
			Map.of("conversationType", "TEAM_LEAD_CHAT"),
			List.of(),
			Map.of("status", "AVAILABLE"),
			tasks,
			Map.of("status", "NOT_AVAILABLE"),
			Map.of("status", "NOT_AVAILABLE"),
			List.of()
		);
	}

	private static AiConversationPromptContext contextWithQuestionPosts(List<Map<String, Object>> posts) {
		return new AiConversationPromptContext(
			Map.of("status", "AVAILABLE", "topic", "Spring Boot"),
			Map.of("status", "AVAILABLE"),
			Map.of("conversationType", "TEAM_LEAD_CHAT"),
			List.of(),
			Map.of("status", "AVAILABLE"),
			List.of(),
			Map.of("status", "NOT_AVAILABLE"),
			Map.of("status", "NOT_AVAILABLE"),
			posts
		);
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
				Map.of(
					"status", "AVAILABLE",
					"retrospectiveStatus", "COMPLETED",
					"aiFeedback", Map.of("summary", "트랜잭션 복습이 필요합니다."),
					"nextWeekAdjustment", Map.of("focus", "JPA 트랜잭션 보강")
				)
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
