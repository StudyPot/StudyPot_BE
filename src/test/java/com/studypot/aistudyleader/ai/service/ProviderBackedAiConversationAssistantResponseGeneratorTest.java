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
				Map.of("conversationType", "TEAM_LEAD_CHAT", "summary", "이전 요약"),
				List.of(Map.of("senderType", "USER", "content", content)),
				Map.of("status", "AVAILABLE", "weekId", WEEK_ID.toString(), "title", "2주차"),
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
