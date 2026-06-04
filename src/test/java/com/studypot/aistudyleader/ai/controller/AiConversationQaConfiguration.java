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
import java.util.LinkedHashMap;
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
	private static final UUID MISSING_CONTEXT_GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000009312");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000009303");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000009304");

	@Bean
	AiConversationRepository qaAiConversationRepository() {
		return new InMemoryQaAiConversationRepository();
	}

	@Bean
	AiConversationAssistantResponseGenerator qaAiConversationAssistantResponseGenerator() {
		return request -> {
			AiConversationPromptContext context = request.promptContext();
			String content = request.userMessage().content();
			String message;
			String summary;
			if ("NOT_AVAILABLE".equals(context.week().get("status"))) {
				message = "DB에서 현재 커리큘럼이나 주차 기록을 확인하지 못했어요. 임의로 계획을 만들기 전에 이 스터디의 현재 주차를 DB에 먼저 등록해줄래요?";
				summary = "DB에 현재 주차 컨텍스트가 없어 추가 확인을 요청했습니다.";
			} else if (content.contains("React Native") || content.contains("DB에 없")) {
				message = "DB에서 React Native 과제 완료 기록은 확인되지 않아요. 지금 확인되는 건 Spring Boot 스터디의 2주차 JPA/트랜잭션 실습 기록이라, 그 기록 기준으로 다음 행동만 잡겠습니다.";
				summary = "DB에 없는 사용자 주장 확인을 거부하고 DB 기록 기준으로 답했습니다.";
			} else {
				message = "지금 Spring Boot 스터디의 백엔드 커리큘럼을 보고 있어요. DB상 현재 2주차 JPA 실습 중이고 트랜잭션 실습이 지연된 상태라, 오늘은 그 실습 하나만 먼저 끝내는 게 좋겠습니다.";
				summary = "Spring Boot 2주차 JPA 실습 지연과 트랜잭션 실습 우선순위를 논의했습니다.";
			}
			return new AiConversationAssistantResponse(
				message,
				summary,
				Map.of(
					"qa", true,
					"studyGroupStatus", context.studyGroup().getOrDefault("status", "UNKNOWN"),
					"curriculumStatus", context.curriculum().getOrDefault("status", "UNKNOWN"),
					"effectiveWeekSource", context.week().getOrDefault("effectiveWeekSource", "UNKNOWN")
				),
				new LlmStructuredResponse(
					LlmProvider.OPENAI,
					"qa-fake-provider",
					"{\"message\":\"" + message + "\",\"conversationSummary\":\"" + summary + "\"}",
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
		};
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

		private final Map<UUID, AiConversation> conversations = new LinkedHashMap<>();
		private final List<AiConversationMessage> messages = new ArrayList<>();
		private String summary = "JPA 실습 조정 대화";

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			return GROUP_ID.equals(groupId) || MISSING_CONTEXT_GROUP_ID.equals(groupId);
		}

		@Override
		public Optional<AiConversationMembershipContext> findMembership(UUID groupId, UUID userId) {
			if ((!GROUP_ID.equals(groupId) && !MISSING_CONTEXT_GROUP_ID.equals(groupId)) || !USER_ID.equals(userId)) {
				return Optional.empty();
			}
			return Optional.of(new AiConversationMembershipContext(
				groupId,
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
			return conversations.values().stream()
				.filter(conversation -> conversation.groupId().equals(groupId))
				.filter(conversation -> conversation.memberId().equals(memberId))
				.filter(conversation -> conversation.conversationType() == AiConversationType.TEAM_LEAD_CHAT)
				.filter(conversation -> conversation.status() == AiConversationStatus.OPEN)
				.filter(conversation -> conversation.curriculumWeekId() == null)
				.filter(conversation -> conversation.retrospectiveId() == null)
				.findFirst();
		}

		@Override
		public boolean insertConversation(AiConversation conversation) {
			conversations.put(conversation.id(), conversation);
			return true;
		}

		@Override
		public boolean existsConversation(UUID conversationId) {
			return conversations.containsKey(conversationId);
		}

		@Override
		public Optional<AiConversationMessageContext> findMessageContext(UUID conversationId, UUID userId) {
			AiConversation conversation = conversations.get(conversationId);
			if (conversation == null || !USER_ID.equals(userId)) {
				return Optional.empty();
			}
			return Optional.of(new AiConversationMessageContext(
				conversation.id(),
				conversation.groupId(),
				MEMBER_ID,
				conversation.curriculumWeekId(),
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
			if (!conversations.containsKey(conversationId)) {
				return List.of();
			}
			return messages.stream().limit(limit).toList();
		}

		@Override
		public AiConversationPromptContext findPromptContext(AiConversationMessageContext context, int recentMessageLimit) {
			boolean missingContext = MISSING_CONTEXT_GROUP_ID.equals(context.groupId());
			Map<String, Object> week = missingContext
				? Map.of("status", "NOT_AVAILABLE", "effectiveWeekSource", "NOT_AVAILABLE")
				: Map.<String, Object>of(
					"status", "AVAILABLE",
					"weekNumber", 2,
					"title", "2주차",
					"sprintGoal", "JPA 실습 안정화",
					"effectiveWeekSource", context.curriculumWeekId() == null ? "CURRENT_WEEK" : "CONVERSATION_WEEK"
				);
			return new AiConversationPromptContext(
				Map.<String, Object>of(
					"status", "AVAILABLE",
					"name", "백엔드 스터디",
					"topic", missingContext ? "NOT_AVAILABLE" : "Spring Boot",
					"detailKeywords", missingContext ? List.of() : List.of("JPA", "백엔드"),
					"groupStatus", "ACTIVE"
				),
				missingContext
					? Map.of("status", "NOT_AVAILABLE")
					: Map.<String, Object>of(
						"status", "AVAILABLE",
						"title", "백엔드 커리큘럼",
						"totalWeeks", 4,
						"onboardingSummary", "Spring Boot와 JPA를 함께 학습합니다.",
						"curriculumStatus", "ACTIVE"
					),
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
				week,
				missingContext
					? List.of()
					: List.of(Map.of(
						"title", "JPA 실습",
						"completionStatus", "INCOMPLETE",
						"incompleteReason", "트랜잭션 실습이 지연되었습니다."
					)),
				missingContext
					? Map.of("status", "NOT_AVAILABLE")
					: Map.of(
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
			return conversations.containsKey(conversationId);
		}
	}
}
