package com.studypot.aistudyleader.ai.controller;

import com.studypot.aistudyleader.ai.domain.AiConversation;
import com.studypot.aistudyleader.ai.domain.AiConversationMembershipContext;
import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageContext;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageCursor;
import com.studypot.aistudyleader.ai.domain.AiConversationPromptContext;
import com.studypot.aistudyleader.ai.domain.AiConversationStatus;
import com.studypot.aistudyleader.ai.domain.AiConversationType;
import com.studypot.aistudyleader.ai.domain.AiRetrospectiveReference;
import com.studypot.aistudyleader.ai.repository.AiConversationRepository;
import com.studypot.aistudyleader.ai.service.AiConversationAssistantResponse;
import com.studypot.aistudyleader.ai.service.AiConversationAssistantResponseGenerator;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsage;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "studypot.qa.ai-conversation", name = "enabled", havingValue = "true")
class AiConversationQaConfiguration {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000009301");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000009302");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000009303");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000009304");
	private static final UUID CONVERSATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000009306");
	@Bean
	AiConversationRepository qaAiConversationRepository() {
		return new InMemoryQaAiConversationRepository();
	}

	@Bean
	AiConversationAssistantResponseGenerator qaAiConversationAssistantResponseGenerator() {
		return request -> new AiConversationAssistantResponse(
			"지금 JPA 실습이 계속 밀린 상황을 확인했어요. 이번 주 DB 기록을 보면 필수 실습 하나를 먼저 끝내고, 남은 읽기는 다음 주 초로 미루는 게 좋겠습니다.",
			"JPA 실습 지연과 필수 과제 우선순위 조정을 논의했습니다.",
			Map.of("qa", true),
			new LlmStructuredResponse(
				LlmProvider.OPENAI,
				"qa-fake-provider",
				"""
					{"message":"지금 JPA 실습이 계속 밀린 상황을 확인했어요. 이번 주 DB 기록을 보면 필수 실습 하나를 먼저 끝내고, 남은 읽기는 다음 주 초로 미루는 게 좋겠습니다.","conversationSummary":"JPA 실습 지연과 필수 과제 우선순위 조정을 논의했습니다."}""",
				120,
				64,
				BigDecimal.ZERO,
				12,
				LlmUsageStatus.SUCCESS,
				null,
				Map.of("purpose", "TEAM_LEAD_CHAT", "conversationId", request.messageContext().conversationId().toString()),
				"QA fake AI conversation response."
			)
		);
	}

	@Bean
	LlmUsageRecorder qaLlmUsageRecorder() {
		return new LlmUsageRecorder() {
			@Override
			public void record(LlmUsage usage) {
			}
		};
	}

	private static final class InMemoryQaAiConversationRepository implements AiConversationRepository {

		private final List<AiConversationMessage> messages = new ArrayList<>();
		private String summary = "JPA 실습 조정 대화";

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			return GROUP_ID.equals(groupId);
		}

		@Override
		public Optional<AiConversationMembershipContext> findMembership(UUID groupId, UUID userId) {
			if (!GROUP_ID.equals(groupId) || !USER_ID.equals(userId)) {
				return Optional.empty();
			}
			return Optional.of(new AiConversationMembershipContext(
				GROUP_ID,
				MEMBER_ID,
				StudyGroupStatus.ACTIVE,
				GroupMemberPermission.MEMBER,
				GroupMemberStatus.ACTIVE
			));
		}

		@Override
		public Optional<UUID> findWeekGroupId(UUID weekId) {
			return WEEK_ID.equals(weekId) ? Optional.of(GROUP_ID) : Optional.empty();
		}

		@Override
		public Optional<AiRetrospectiveReference> findRetrospectiveReference(UUID retrospectiveId) {
			return Optional.empty();
		}

		@Override
		public Optional<AiConversation> findOpenTeamLeadConversation(UUID groupId, UUID memberId) {
			return Optional.empty();
		}

		@Override
		public boolean insertConversation(AiConversation conversation) {
			return true;
		}

		@Override
		public boolean existsConversation(UUID conversationId) {
			return CONVERSATION_ID.equals(conversationId);
		}

		@Override
		public Optional<AiConversationMessageContext> findMessageContext(UUID conversationId, UUID userId) {
			if (!CONVERSATION_ID.equals(conversationId) || !USER_ID.equals(userId)) {
				return Optional.empty();
			}
			return Optional.of(new AiConversationMessageContext(
				CONVERSATION_ID,
				GROUP_ID,
				MEMBER_ID,
				WEEK_ID,
				null,
				AiConversationType.TEAM_LEAD_CHAT,
				summary,
				AiConversationStatus.OPEN,
				StudyGroupStatus.ACTIVE,
				GroupMemberStatus.ACTIVE
			));
		}

		@Override
		public boolean insertMessage(AiConversationMessage message) {
			messages.add(message);
			return true;
		}

		@Override
		public List<AiConversationMessage> findMessages(UUID conversationId, AiConversationMessageCursor cursor, int limit) {
			if (!CONVERSATION_ID.equals(conversationId)) {
				return List.of();
			}
			return messages.stream().limit(limit).toList();
		}

		@Override
		public AiConversationPromptContext findPromptContext(AiConversationMessageContext context, int recentMessageLimit) {
			return new AiConversationPromptContext(
				Map.of(
					"conversationType", "TEAM_LEAD_CHAT",
					"summary", summary
				),
				messages.stream()
					.limit(recentMessageLimit)
					.map(message -> Map.<String, Object>of(
						"senderType", message.senderType().name(),
						"content", message.content(),
						"createdAt", message.createdAt().toString()
					))
					.toList(),
				Map.of(
					"status", "AVAILABLE",
					"weekNumber", 2,
					"title", "2주차",
					"sprintGoal", "JPA 실습 안정화"
				),
				List.of(Map.of(
					"title", "JPA 실습",
					"completionStatus", "INCOMPLETE",
					"incompleteReason", "트랜잭션 실습이 지연되었습니다."
				)),
				Map.of(
					"status", "AVAILABLE",
					"progressStatus", "INCOMPLETE",
					"incompleteReason", "실습 시간이 부족했습니다."
				),
				Map.of("status", "NOT_AVAILABLE")
			);
		}

		@Override
		public boolean updateConversationSummary(UUID conversationId, String summary, Instant updatedAt) {
			this.summary = summary;
			return CONVERSATION_ID.equals(conversationId);
		}
	}
}
