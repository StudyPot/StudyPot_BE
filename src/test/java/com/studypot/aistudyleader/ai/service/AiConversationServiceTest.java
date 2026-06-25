package com.studypot.aistudyleader.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.ai.domain.AiConversation;
import com.studypot.aistudyleader.ai.domain.AiConversationMembershipContext;
import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageContext;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageCursor;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageSenderType;
import com.studypot.aistudyleader.ai.domain.AiConversationPromptContext;
import com.studypot.aistudyleader.ai.domain.AiConversationStatus;
import com.studypot.aistudyleader.ai.domain.AiConversationType;
import com.studypot.aistudyleader.ai.domain.AiRetrospectiveReference;
import com.studypot.aistudyleader.ai.repository.AiConversationRepository;
import com.studypot.aistudyleader.global.api.CursorPageResponse;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsage;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.service.LlmCallFailure;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiConversationServiceTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000009101");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000009102");
	private static final UUID OTHER_GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000009103");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000009104");
	private static final UUID OTHER_MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000009105");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000009106");
	private static final UUID RETROSPECTIVE_ID = UUID.fromString("018f0000-0000-7000-8000-000000009107");
	private static final UUID CONVERSATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000009108");
	private static final UUID MESSAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000009109");
	private static final UUID ASSISTANT_MESSAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000009110");
	private static final UUID LLM_USAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000009111");
	private static final Instant NOW = Instant.parse("2026-05-13T00:45:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Test
	void openTeamLeadChatCreatesOpenConversationForActiveMember() {
		CapturingRepository repository = new CapturingRepository();
		AiConversationService service = service(repository, CONVERSATION_ID);

		AiConversation result = service.openConversation(new OpenAiConversationCommand(
			USER_ID,
			GROUP_ID,
			AiConversationType.TEAM_LEAD_CHAT,
			null,
			null
		));

		assertThat(result.id()).isEqualTo(CONVERSATION_ID);
		assertThat(result.groupId()).isEqualTo(GROUP_ID);
		assertThat(result.memberId()).isEqualTo(MEMBER_ID);
		assertThat(result.conversationType()).isEqualTo(AiConversationType.TEAM_LEAD_CHAT);
		assertThat(result.status()).isEqualTo(AiConversationStatus.OPEN);
		assertThat(result.curriculumWeekId()).isNull();
		assertThat(result.retrospectiveId()).isNull();
		assertThat(result.summary()).isEmpty();
		assertThat(result.openedAt()).isEqualTo(NOW);
		assertThat(repository.insertedConversation).isSameAs(result);
	}

	@Test
	void openTeamLeadChatReusesExistingOpenConversationForMember() {
		CapturingRepository repository = new CapturingRepository();
		AiConversation existing = AiConversation.open(
			CONVERSATION_ID,
			GROUP_ID,
			MEMBER_ID,
			null,
			null,
			AiConversationType.TEAM_LEAD_CHAT,
			NOW.minusSeconds(60)
		);
		repository.openTeamLeadConversation = existing;
		AiConversationService service = service(repository);

		AiConversation result = service.openConversation(new OpenAiConversationCommand(
			USER_ID,
			GROUP_ID,
			AiConversationType.TEAM_LEAD_CHAT,
			null,
			null
		));

		assertThat(result).isSameAs(existing);
		assertThat(repository.insertedConversation).isNull();
	}

	@Test
	void openTeamLeadChatWithWeekCreatesNewConversationEvenWhenOrdinaryExists() {
		UUID newConversationId = UUID.fromString("018f0000-0000-7000-8000-000000009113");
		CapturingRepository repository = new CapturingRepository();
		repository.openTeamLeadConversation = AiConversation.open(
			CONVERSATION_ID,
			GROUP_ID,
			MEMBER_ID,
			null,
			null,
			AiConversationType.TEAM_LEAD_CHAT,
			NOW.minusSeconds(60)
		);
		AiConversationService service = service(repository, newConversationId);

		AiConversation result = service.openConversation(new OpenAiConversationCommand(
			USER_ID,
			GROUP_ID,
			AiConversationType.TEAM_LEAD_CHAT,
			WEEK_ID,
			null
		));

		assertThat(result.id()).isEqualTo(newConversationId);
		assertThat(result.curriculumWeekId()).isEqualTo(WEEK_ID);
		assertThat(repository.insertedConversation).isSameAs(result);
	}

	@Test
	void openRetrospectiveConversationLinksSameMemberRetrospectiveAndInfersWeek() {
		CapturingRepository repository = new CapturingRepository();
		repository.retrospectiveReference = new AiRetrospectiveReference(GROUP_ID, MEMBER_ID, WEEK_ID);
		UUID seedMessageId = UUID.fromString("018f0000-0000-7000-8000-000000009201");
		AiConversationService service = service(repository, CONVERSATION_ID, seedMessageId);

		AiConversation result = service.openConversation(new OpenAiConversationCommand(
			USER_ID,
			GROUP_ID,
			AiConversationType.RETROSPECTIVE,
			null,
			RETROSPECTIVE_ID
		));

		assertThat(result.conversationType()).isEqualTo(AiConversationType.RETROSPECTIVE);
		assertThat(result.curriculumWeekId()).isEqualTo(WEEK_ID);
		assertThat(result.retrospectiveId()).isEqualTo(RETROSPECTIVE_ID);
		assertThat(repository.insertedConversation).isSameAs(result);
		assertThat(repository.insertedMessage).isNotNull();
		assertThat(repository.insertedMessage.senderType())
			.isEqualTo(com.studypot.aistudyleader.ai.domain.AiConversationMessageSenderType.ASSISTANT);
		assertThat(repository.insertedMessage.content()).contains("회고");
	}

	@Test
	void openConversationRejectsPendingMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = new AiConversationMembershipContext(
			GROUP_ID,
			MEMBER_ID,
			StudyGroupStatus.ACTIVE,
			GroupMemberPermission.MEMBER,
			GroupMemberStatus.PENDING_ONBOARDING
		);
		AiConversationService service = service(repository, CONVERSATION_ID);

		assertThatThrownBy(() -> service.openConversation(new OpenAiConversationCommand(
				USER_ID,
				GROUP_ID,
				AiConversationType.TEAM_LEAD_CHAT,
				null,
				null
			)))
			.isInstanceOf(AiConversationAccessDeniedException.class)
			.hasMessage("active group membership is required to open an AI conversation.");
		assertThat(repository.insertedConversation).isNull();
	}

	@Test
	void openConversationRejectsLeftMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = new AiConversationMembershipContext(
			GROUP_ID,
			MEMBER_ID,
			StudyGroupStatus.ACTIVE,
			GroupMemberPermission.MEMBER,
			GroupMemberStatus.LEFT
		);
		AiConversationService service = service(repository, CONVERSATION_ID);

		assertThatThrownBy(() -> service.openConversation(new OpenAiConversationCommand(
				USER_ID,
				GROUP_ID,
				AiConversationType.TEAM_LEAD_CHAT,
				null,
				null
			)))
			.isInstanceOf(AiConversationAccessDeniedException.class)
			.hasMessage("active group membership is required to open an AI conversation.");
		assertThat(repository.insertedConversation).isNull();
	}

	@Test
	void openConversationRejectsMissingStudyGroup() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = false;
		repository.membership = null;
		AiConversationService service = service(repository, CONVERSATION_ID);

		assertThatThrownBy(() -> service.openConversation(new OpenAiConversationCommand(
				USER_ID,
				GROUP_ID,
				AiConversationType.TEAM_LEAD_CHAT,
				null,
				null
			)))
			.isInstanceOf(AiConversationNotFoundException.class)
			.hasMessage("study group was not found.");
	}

	@Test
	void openConversationRejectsCrossGroupWeek() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekGroupId = OTHER_GROUP_ID;
		AiConversationService service = service(repository, CONVERSATION_ID);

		assertThatThrownBy(() -> service.openConversation(new OpenAiConversationCommand(
				USER_ID,
				GROUP_ID,
				AiConversationType.TEAM_LEAD_CHAT,
				WEEK_ID,
				null
			)))
			.isInstanceOf(AiConversationAccessDeniedException.class)
			.hasMessage("curriculum week does not belong to this study group.");
		assertThat(repository.insertedConversation).isNull();
	}

	@Test
	void openConversationRejectsCrossMemberRetrospective() {
		CapturingRepository repository = new CapturingRepository();
		repository.retrospectiveReference = new AiRetrospectiveReference(GROUP_ID, OTHER_MEMBER_ID, WEEK_ID);
		AiConversationService service = service(repository, CONVERSATION_ID);

		assertThatThrownBy(() -> service.openConversation(new OpenAiConversationCommand(
				USER_ID,
				GROUP_ID,
				AiConversationType.RETROSPECTIVE,
				null,
				RETROSPECTIVE_ID
			)))
			.isInstanceOf(AiConversationAccessDeniedException.class)
			.hasMessage("retrospective does not belong to the authenticated member.");
		assertThat(repository.insertedConversation).isNull();
	}

	@Test
	void openConversationRejectsTeamLeadChatWithRetrospectiveReference() {
		CapturingRepository repository = new CapturingRepository();
		AiConversationService service = service(repository, CONVERSATION_ID);

		assertThatThrownBy(() -> service.openConversation(new OpenAiConversationCommand(
				USER_ID,
				GROUP_ID,
				AiConversationType.TEAM_LEAD_CHAT,
				null,
				RETROSPECTIVE_ID
			)))
			.isInstanceOf(InvalidAiConversationRequestException.class)
			.hasMessage("retrospectiveId is only allowed for RETROSPECTIVE conversations.");
		assertThat(repository.insertedConversation).isNull();
	}

	@Test
	void sendUserMessageStoresMessageForOpenConversationMember() {
		CapturingRepository repository = new CapturingRepository();
		AiConversationService service = service(repository, MESSAGE_ID);

		AiConversationMessage result = service.sendMessage(new SendAiConversationMessageCommand(
			USER_ID,
			CONVERSATION_ID,
			"  이번 주 과제 양을 줄이고 싶어요.  "
		));

		assertThat(result.id()).isEqualTo(MESSAGE_ID);
		assertThat(result.conversationId()).isEqualTo(CONVERSATION_ID);
		assertThat(result.senderType()).isEqualTo(AiConversationMessageSenderType.USER);
		assertThat(result.content()).isEqualTo("이번 주 과제 양을 줄이고 싶어요.");
		assertThat(result.llmUsageId()).isNull();
		assertThat(result.metadata()).isEmpty();
		assertThat(result.createdAt()).isEqualTo(NOW);
		assertThat(repository.insertedMessage).isSameAs(result);
	}

	@Test
	void sendUserMessagePublishesUserMessageSavedEvent() {
		CapturingRepository repository = new CapturingRepository();
		CapturingStreamPublisher streamPublisher = new CapturingStreamPublisher();
		AiConversationService service = serviceWithStream(repository, streamPublisher, MESSAGE_ID);

		AiConversationMessage result = service.sendMessage(new SendAiConversationMessageCommand(
			USER_ID,
			CONVERSATION_ID,
			"이번 주 과제 양을 줄이고 싶어요."
		));

		assertThat(streamPublisher.events)
			.extracting(StreamEvent::name)
			.containsExactly(AiConversationStreamPublisher.USER_MESSAGE_SAVED_EVENT);
		assertThat(streamPublisher.messages).containsExactly(result);
	}

	@Test
	void sendMessageGeneratesAssistantResponseAndRecordsSuccessfulUsageWhenProviderIsConfigured() {
		CapturingRepository repository = new CapturingRepository();
		repository.messageContext = new AiConversationMessageContext(
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
		);
		repository.promptContext = new AiConversationPromptContext(
			Map.of("status", "AVAILABLE", "topic", "Spring Boot"),
			Map.of("status", "AVAILABLE", "title", "백엔드 커리큘럼"),
			Map.of("conversationType", "TEAM_LEAD_CHAT"),
			List.of(Map.of("senderType", "USER", "content", "이전 메시지")),
			Map.of("weekId", WEEK_ID.toString(), "title", "2주차", "effectiveWeekSource", "CONVERSATION_WEEK"),
			List.of(Map.of("title", "필수 과제", "completionStatus", "TODO")),
			Map.of("status", "IN_PROGRESS"),
			Map.of("status", "NOT_AVAILABLE")
		);
		FakeAssistantGenerator generator = new FakeAssistantGenerator(new AiConversationAssistantResponse(
			"이번 주 필수 과제 하나를 줄이는 방향으로 조정해볼게요.",
			"과제 양 조정 요청을 확인했고 다음 주 난이도 조절 후보를 제안했습니다.",
			Map.of("nextWeekAdjustmentCandidate", Map.of("difficulty", "LOWER")),
			successResponse()
		));
		CapturingUsageRecorder usageRecorder = new CapturingUsageRecorder();
		AiConversationService service = serviceWithAi(repository, generator, usageRecorder, MESSAGE_ID, LLM_USAGE_ID, ASSISTANT_MESSAGE_ID);

		AiConversationMessage result = service.sendMessage(new SendAiConversationMessageCommand(
			USER_ID,
			CONVERSATION_ID,
			"필수 과제가 너무 많아요."
		));

		assertThat(result.id()).isEqualTo(ASSISTANT_MESSAGE_ID);
		assertThat(result.senderType()).isEqualTo(AiConversationMessageSenderType.ASSISTANT);
		assertThat(result.llmUsageId()).isEqualTo(LLM_USAGE_ID);
		assertThat(result.content()).isEqualTo("이번 주 필수 과제 하나를 줄이는 방향으로 조정해볼게요.");
		assertThat(result.metadata()).containsEntry("retrievalContextVersion", "db-first-v1");
		assertThat(repository.insertedMessages)
			.extracting(AiConversationMessage::senderType)
			.containsExactly(AiConversationMessageSenderType.USER, AiConversationMessageSenderType.ASSISTANT);
		assertThat(generator.request.userMessage().content()).isEqualTo("필수 과제가 너무 많아요.");
		assertThat(generator.request.promptContext()).isSameAs(repository.promptContext);
		assertThat(usageRecorder.recorded).hasSize(1);
		assertThat(usageRecorder.recorded.getFirst().id()).isEqualTo(LLM_USAGE_ID);
		assertThat(usageRecorder.recorded.getFirst().purpose()).isEqualTo(LlmUsagePurpose.TEAM_LEAD_CHAT);
		assertThat(usageRecorder.recorded.getFirst().status()).isEqualTo(LlmUsageStatus.SUCCESS);
		assertThat(repository.updatedSummary).isEqualTo("""
			이전 요약
			과제 양 조정 요청을 확인했고 다음 주 난이도 조절 후보를 제안했습니다.""");
	}

	@Test
	void sendMessagePublishesAssistantLifecycleEventsWhenProviderSucceeds() {
		CapturingRepository repository = new CapturingRepository();
		FakeAssistantGenerator generator = new FakeAssistantGenerator(new AiConversationAssistantResponse(
			"이번 주 필수 과제 하나를 줄이는 방향으로 조정해볼게요.",
			null,
			Map.of(),
			successResponse()
		));
		CapturingUsageRecorder usageRecorder = new CapturingUsageRecorder();
		CapturingStreamPublisher streamPublisher = new CapturingStreamPublisher();
		AiConversationService service = serviceWithAiAndStream(
			repository,
			generator,
			usageRecorder,
			streamPublisher,
			MESSAGE_ID,
			LLM_USAGE_ID,
			ASSISTANT_MESSAGE_ID
		);

		AiConversationMessage result = service.sendMessage(new SendAiConversationMessageCommand(
			USER_ID,
			CONVERSATION_ID,
			"필수 과제가 너무 많아요."
		));

		assertThat(result.id()).isEqualTo(ASSISTANT_MESSAGE_ID);
		assertThat(streamPublisher.events)
			.extracting(StreamEvent::name)
			.containsExactly(
				AiConversationStreamPublisher.USER_MESSAGE_SAVED_EVENT,
				AiConversationStreamPublisher.ASSISTANT_GENERATION_STARTED_EVENT,
				AiConversationStreamPublisher.ASSISTANT_MESSAGE_CREATED_EVENT
			);
		assertThat(streamPublisher.messages)
			.extracting(AiConversationMessage::senderType)
			.containsExactly(AiConversationMessageSenderType.USER, AiConversationMessageSenderType.ASSISTANT);
	}

	@Test
	void sendMessageKeepsUserMessageAndFailedUsageWithoutAssistantWhenProviderFails() {
		CapturingRepository repository = new CapturingRepository();
		LlmCallFailure failure = new LlmCallFailure(
			LlmUsagePurpose.TEAM_LEAD_CHAT,
			LlmProvider.OPENAI,
			"gpt-test",
			42,
			0,
			BigDecimal.ZERO,
			1_000,
			LlmUsageStatus.TIMEOUT,
			"AI_CHAT_TIMEOUT",
			Map.of("purpose", "TEAM_LEAD_CHAT", "conversationId", CONVERSATION_ID.toString()),
			"AI chat response timed out."
		);
		FakeAssistantGenerator generator = new FakeAssistantGenerator(new AiConversationResponseGenerationException(
			"AI conversation response generation failed.",
			failure
		));
		CapturingUsageRecorder usageRecorder = new CapturingUsageRecorder();
		AiConversationService service = serviceWithAi(repository, generator, usageRecorder, MESSAGE_ID, LLM_USAGE_ID, ASSISTANT_MESSAGE_ID);

		assertThatThrownBy(() -> service.sendMessage(new SendAiConversationMessageCommand(
				USER_ID,
				CONVERSATION_ID,
				"응답 실패도 저장되어야 합니다."
			)))
			.isInstanceOf(AiConversationResponseGenerationException.class)
			.hasMessage("AI conversation response generation failed.");

		assertThat(repository.insertedMessages)
			.extracting(AiConversationMessage::senderType)
			.containsExactly(AiConversationMessageSenderType.USER);
		assertThat(usageRecorder.recorded).hasSize(1);
		assertThat(usageRecorder.recorded.getFirst().id()).isEqualTo(LLM_USAGE_ID);
		assertThat(usageRecorder.recorded.getFirst().status()).isEqualTo(LlmUsageStatus.TIMEOUT);
		assertThat(usageRecorder.recorded.getFirst().errorCode()).isEqualTo("AI_CHAT_TIMEOUT");
		assertThat(repository.updatedSummary).isNull();
	}

	@Test
	void sendMessagePublishesFailureEventWhenProviderFails() {
		CapturingRepository repository = new CapturingRepository();
		LlmCallFailure failure = new LlmCallFailure(
			LlmUsagePurpose.TEAM_LEAD_CHAT,
			LlmProvider.OPENAI,
			"gpt-test",
			42,
			0,
			BigDecimal.ZERO,
			1_000,
			LlmUsageStatus.TIMEOUT,
			"AI_CHAT_TIMEOUT",
			Map.of("purpose", "TEAM_LEAD_CHAT", "conversationId", CONVERSATION_ID.toString()),
			"AI chat response timed out."
		);
		FakeAssistantGenerator generator = new FakeAssistantGenerator(new AiConversationResponseGenerationException(
			"AI conversation response generation failed.",
			failure
		));
		CapturingUsageRecorder usageRecorder = new CapturingUsageRecorder();
		CapturingStreamPublisher streamPublisher = new CapturingStreamPublisher();
		AiConversationService service = serviceWithAiAndStream(
			repository,
			generator,
			usageRecorder,
			streamPublisher,
			MESSAGE_ID,
			LLM_USAGE_ID,
			ASSISTANT_MESSAGE_ID
		);

		assertThatThrownBy(() -> service.sendMessage(new SendAiConversationMessageCommand(
				USER_ID,
				CONVERSATION_ID,
				"응답 실패도 저장되어야 합니다."
			)))
			.isInstanceOf(AiConversationResponseGenerationException.class);

		assertThat(streamPublisher.events)
			.extracting(StreamEvent::name)
			.containsExactly(
				AiConversationStreamPublisher.USER_MESSAGE_SAVED_EVENT,
				AiConversationStreamPublisher.ASSISTANT_GENERATION_STARTED_EVENT,
				AiConversationStreamPublisher.ASSISTANT_GENERATION_FAILED_EVENT
			);
		assertThat(streamPublisher.failures)
			.singleElement()
			.satisfies(failed -> {
				assertThat(failed.conversationId()).isEqualTo(CONVERSATION_ID);
				assertThat(failed.errorCode()).isEqualTo("AI_CHAT_TIMEOUT");
			});
	}

	@Test
	void streamPublishFailureDoesNotFailMessagePersistence() {
		CapturingRepository repository = new CapturingRepository();
		AiConversationStreamPublisher streamPublisher = new ThrowingStreamPublisher();
		AiConversationService service = serviceWithStream(repository, streamPublisher, MESSAGE_ID);

		AiConversationMessage result = service.sendMessage(new SendAiConversationMessageCommand(
			USER_ID,
			CONVERSATION_ID,
			"스트림 실패와 무관하게 저장되어야 합니다."
		));

		assertThat(result).isSameAs(repository.insertedMessage);
		assertThat(result.senderType()).isEqualTo(AiConversationMessageSenderType.USER);
	}

	@Test
	void validateConversationStreamAccessRejectsOtherUserConversation() {
		CapturingRepository repository = new CapturingRepository();
		repository.messageContext = null;
		repository.conversationExists = true;
		AiConversationService service = service(repository, MESSAGE_ID);

		assertThatThrownBy(() -> service.validateConversationStreamAccess(new SubscribeAiConversationStreamQuery(
				USER_ID,
				CONVERSATION_ID
			)))
			.isInstanceOf(AiConversationAccessDeniedException.class)
			.hasMessage("authenticated user is not a member of this AI conversation.");
	}

	@Test
	void sendUserMessageRejectsConversationOwnedByDifferentUser() {
		CapturingRepository repository = new CapturingRepository();
		repository.messageContext = null;
		repository.conversationExists = true;
		AiConversationService service = service(repository, MESSAGE_ID);

		assertThatThrownBy(() -> service.sendMessage(new SendAiConversationMessageCommand(
				USER_ID,
				CONVERSATION_ID,
				"권한 없는 대화"
			)))
			.isInstanceOf(AiConversationAccessDeniedException.class)
			.hasMessage("authenticated user is not a member of this AI conversation.");
		assertThat(repository.insertedMessage).isNull();
	}

	@Test
	void sendUserMessageRejectsClosedConversation() {
		CapturingRepository repository = new CapturingRepository();
		repository.messageContext = new AiConversationMessageContext(
			CONVERSATION_ID,
			GROUP_ID,
			MEMBER_ID,
			AiConversationStatus.CLOSED,
			StudyGroupStatus.ACTIVE,
			GroupMemberStatus.ACTIVE
		);
		AiConversationService service = service(repository, MESSAGE_ID);

		assertThatThrownBy(() -> service.sendMessage(new SendAiConversationMessageCommand(
				USER_ID,
				CONVERSATION_ID,
				"닫힌 대화"
			)))
			.isInstanceOf(AiConversationMutationRejectedException.class)
			.hasMessage("AI conversation is closed.");
		assertThat(repository.insertedMessage).isNull();
	}

	@Test
	void listMessagesReturnsCursorPageScopedToConversationMember() {
		CapturingRepository repository = new CapturingRepository();
		AiConversationMessage userMessage = AiConversationMessage.userMessage(
			MESSAGE_ID,
			CONVERSATION_ID,
			"사용자 메시지",
			NOW.minusSeconds(20)
		);
		AiConversationMessage assistantMessage = new AiConversationMessage(
			ASSISTANT_MESSAGE_ID,
			CONVERSATION_ID,
			LLM_USAGE_ID,
			AiConversationMessageSenderType.ASSISTANT,
			"AI 응답",
			java.util.Map.of("retrievalContextVersion", "db-first-v1"),
			NOW.minusSeconds(10)
		);
		AiConversationMessage systemMessage = new AiConversationMessage(
			UUID.fromString("018f0000-0000-7000-8000-000000009112"),
			CONVERSATION_ID,
			null,
			AiConversationMessageSenderType.SYSTEM,
			"시스템 메모",
			java.util.Map.of(),
			NOW
		);
		repository.messages = List.of(userMessage, assistantMessage, systemMessage);
		AiConversationService service = service(repository, MESSAGE_ID);

		CursorPageResponse<AiConversationMessage> result = service.listMessages(new ListAiConversationMessagesQuery(
			USER_ID,
			CONVERSATION_ID,
			null,
			2
		));

		assertThat(result.items()).containsExactly(userMessage, assistantMessage);
		assertThat(result.pageInfo().hasNext()).isTrue();
		assertThat(result.pageInfo().nextCursor()).isNotBlank();
		assertThat(repository.lastMessageLimit).isEqualTo(3);
	}

	@Test
	void listMessagesRejectsInvalidCursor() {
		CapturingRepository repository = new CapturingRepository();
		AiConversationService service = service(repository, MESSAGE_ID);

		assertThatThrownBy(() -> service.listMessages(new ListAiConversationMessagesQuery(
				USER_ID,
				CONVERSATION_ID,
				"not-a-cursor",
				20
			)))
			.isInstanceOf(InvalidAiConversationRequestException.class)
			.hasMessage("cursor is invalid.");
	}

	@Test
	void listMessagesRejectsLeftMemberBeforeReturningRawMessages() {
		CapturingRepository repository = new CapturingRepository();
		repository.messageContext = new AiConversationMessageContext(
			CONVERSATION_ID,
			GROUP_ID,
			MEMBER_ID,
			AiConversationStatus.OPEN,
			StudyGroupStatus.ACTIVE,
			GroupMemberStatus.LEFT
		);
		repository.messages = List.of(AiConversationMessage.userMessage(
			MESSAGE_ID,
			CONVERSATION_ID,
			"LEFT 멤버에게 raw message를 반환하면 안 됩니다.",
			NOW
		));
		AiConversationService service = service(repository, MESSAGE_ID);

		assertThatThrownBy(() -> service.listMessages(new ListAiConversationMessagesQuery(
				USER_ID,
				CONVERSATION_ID,
				null,
				20
			)))
			.isInstanceOf(AiConversationAccessDeniedException.class)
			.hasMessage("active group membership is required to read AI conversation messages.");
		assertThat(repository.lastMessageLimit).isZero();
	}

	@Test
	void confirmShareQuestionPostsToBoardAndMarksExecuted() {
		UUID postId = UUID.fromString("018f0000-0000-7000-8000-0000000091aa");
		CapturingRepository repository = new CapturingRepository();
		Map<String, Object> pendingAction = new LinkedHashMap<>();
		pendingAction.put("type", "SHARE_QUESTION");
		pendingAction.put("status", "PENDING");
		pendingAction.put("question", Map.of("title", "JPA 영속성 컨텍스트란?", "summary", "질문과 답변 요약입니다."));
		repository.messageForAction = AiConversationMessage.assistantSeedMessage(
			ASSISTANT_MESSAGE_ID,
			CONVERSATION_ID,
			"답변이에요. 이 질문은 다른 분들께도 도움이 될 것 같아요. 게시판에 올려둘까요?",
			Map.of("pendingAction", pendingAction),
			NOW
		);
		RecordingBoardGateway gateway = new RecordingBoardGateway(postId);
		AiConversationService service = serviceWithBoard(repository, gateway, MESSAGE_ID);

		AiConversationMessage result = service.decideMessageAction(new DecideAiConversationMessageActionCommand(
			USER_ID,
			CONVERSATION_ID,
			ASSISTANT_MESSAGE_ID,
			AiConversationMessageActionDecision.CONFIRM
		));

		assertThat(gateway.calls).hasSize(1);
		assertThat(gateway.lastUserId).isEqualTo(USER_ID);
		assertThat(gateway.lastGroupId).isEqualTo(GROUP_ID);
		assertThat(gateway.lastTitle).isEqualTo("JPA 영속성 컨텍스트란?");
		assertThat(gateway.lastContent).isEqualTo("질문과 답변 요약입니다.");
		assertThat(repository.updatedMetadataMessageId).isEqualTo(ASSISTANT_MESSAGE_ID);
		assertThat(actionStatus(repository.updatedMetadata)).isEqualTo("EXECUTED");
		assertThat(repository.insertedMessages).hasSize(1);
		assertThat(repository.insertedMessages.getFirst().content()).contains("게시판에 올렸어");
		assertThat(actionStatus(result.metadata())).isEqualTo("EXECUTED");
	}

	@Test
	void confirmShareQuestionWithInstructionPostsRefinedDraft() {
		UUID postId = UUID.fromString("018f0000-0000-7000-8000-0000000091bb");
		CapturingRepository repository = new CapturingRepository();
		Map<String, Object> pendingAction = new LinkedHashMap<>();
		pendingAction.put("type", "SHARE_QUESTION");
		pendingAction.put("status", "PENDING");
		pendingAction.put("question", Map.of("title", "원래 제목", "summary", "원래 요약"));
		repository.messageForAction = AiConversationMessage.assistantSeedMessage(
			ASSISTANT_MESSAGE_ID,
			CONVERSATION_ID,
			"답변이에요. 게시판에 올려둘까요?",
			Map.of("pendingAction", pendingAction),
			NOW
		);
		RecordingBoardGateway gateway = new RecordingBoardGateway(postId);
		RecordingQuestionRefiner refiner = new RecordingQuestionRefiner(new RefinedQuestionPost("다듬은 제목", "다듬은 본문"));
		AiConversationService service = serviceWithBoardAndRefiner(repository, gateway, refiner, MESSAGE_ID);

		service.decideMessageAction(new DecideAiConversationMessageActionCommand(
			USER_ID,
			CONVERSATION_ID,
			ASSISTANT_MESSAGE_ID,
			AiConversationMessageActionDecision.CONFIRM,
			"예시 코드 포함해서 더 짧게"
		));

		assertThat(refiner.lastInstruction).isEqualTo("예시 코드 포함해서 더 짧게");
		assertThat(gateway.lastTitle).isEqualTo("다듬은 제목");
		assertThat(gateway.lastContent).isEqualTo("다듬은 본문");
		assertThat(actionStatus(repository.updatedMetadata)).isEqualTo("EXECUTED");
	}

	@Test
	void confirmCompleteTaskInvokesCurriculumGateway() {
		UUID taskId = UUID.fromString("018f0000-0000-7000-8000-0000000091cc");
		CapturingRepository repository = new CapturingRepository();
		Map<String, Object> pendingAction = new LinkedHashMap<>();
		pendingAction.put("type", "COMPLETE_TASK");
		pendingAction.put("status", "PENDING");
		pendingAction.put("taskId", taskId.toString());
		pendingAction.put("title", "JPA 실습");
		pendingAction.put("completionStatus", "DONE");
		repository.messageForAction = AiConversationMessage.assistantSeedMessage(
			ASSISTANT_MESSAGE_ID,
			CONVERSATION_ID,
			"이 과제를 완료로 표시할까요?",
			Map.of("pendingAction", pendingAction),
			NOW
		);
		RecordingCurriculumGateway gateway = new RecordingCurriculumGateway();
		AiConversationService service = serviceWithCurriculum(repository, gateway, MESSAGE_ID);

		service.decideMessageAction(new DecideAiConversationMessageActionCommand(
			USER_ID,
			CONVERSATION_ID,
			ASSISTANT_MESSAGE_ID,
			AiConversationMessageActionDecision.CONFIRM
		));

		assertThat(gateway.calls).isEqualTo(1);
		assertThat(gateway.lastUserId).isEqualTo(USER_ID);
		assertThat(gateway.lastTaskId).isEqualTo(taskId);
		assertThat(gateway.lastStatus).isEqualTo("DONE");
		assertThat(actionStatus(repository.updatedMetadata)).isEqualTo("EXECUTED");
	}

	@Test
	void confirmAddTaskInvokesCurriculumGateway() {
		CapturingRepository repository = new CapturingRepository();
		Map<String, Object> pendingAction = new LinkedHashMap<>();
		pendingAction.put("type", "ADD_TASK");
		pendingAction.put("status", "PENDING");
		pendingAction.put("title", "트랜잭션 실습");
		pendingAction.put("description", "선언적 트랜잭션 예제 따라하기");
		repository.messageForAction = AiConversationMessage.assistantSeedMessage(
			ASSISTANT_MESSAGE_ID,
			CONVERSATION_ID,
			"이번 주에 추가할까요?",
			Map.of("pendingAction", pendingAction),
			NOW
		);
		RecordingCurriculumGateway gateway = new RecordingCurriculumGateway();
		AiConversationService service = serviceWithCurriculum(repository, gateway, MESSAGE_ID);

		service.decideMessageAction(new DecideAiConversationMessageActionCommand(
			USER_ID,
			CONVERSATION_ID,
			ASSISTANT_MESSAGE_ID,
			AiConversationMessageActionDecision.CONFIRM
		));

		assertThat(gateway.addCalls).isEqualTo(1);
		assertThat(gateway.lastUserId).isEqualTo(USER_ID);
		assertThat(gateway.lastAddGroupId).isEqualTo(GROUP_ID);
		assertThat(gateway.lastAddTitle).isEqualTo("트랜잭션 실습");
		assertThat(gateway.lastAddDescription).isEqualTo("선언적 트랜잭션 예제 따라하기");
		assertThat(actionStatus(repository.updatedMetadata)).isEqualTo("EXECUTED");
	}

	@Test
	void rejectShareQuestionMarksRejectedWithoutPosting() {
		CapturingRepository repository = new CapturingRepository();
		Map<String, Object> pendingAction = new LinkedHashMap<>();
		pendingAction.put("type", "SHARE_QUESTION");
		pendingAction.put("status", "PENDING");
		pendingAction.put("question", Map.of("title", "t", "summary", "s"));
		repository.messageForAction = AiConversationMessage.assistantSeedMessage(
			ASSISTANT_MESSAGE_ID,
			CONVERSATION_ID,
			"답변이에요. 게시판에 올려둘까요?",
			Map.of("pendingAction", pendingAction),
			NOW
		);
		RecordingBoardGateway gateway = new RecordingBoardGateway(UUID.randomUUID());
		AiConversationService service = serviceWithBoard(repository, gateway, MESSAGE_ID);

		service.decideMessageAction(new DecideAiConversationMessageActionCommand(
			USER_ID,
			CONVERSATION_ID,
			ASSISTANT_MESSAGE_ID,
			AiConversationMessageActionDecision.REJECT
		));

		assertThat(gateway.calls).isEmpty();
		assertThat(repository.insertedMessages).isEmpty();
		assertThat(actionStatus(repository.updatedMetadata)).isEqualTo("REJECTED");
	}

	@Test
	void decideRejectsWhenMessageHasNoPendingAction() {
		CapturingRepository repository = new CapturingRepository();
		repository.messageForAction = AiConversationMessage.assistantSeedMessage(
			ASSISTANT_MESSAGE_ID,
			CONVERSATION_ID,
			"그냥 답변입니다.",
			Map.of(),
			NOW
		);
		AiConversationService service = serviceWithBoard(repository, new RecordingBoardGateway(UUID.randomUUID()), MESSAGE_ID);

		assertThatThrownBy(() -> service.decideMessageAction(new DecideAiConversationMessageActionCommand(
				USER_ID,
				CONVERSATION_ID,
				ASSISTANT_MESSAGE_ID,
				AiConversationMessageActionDecision.CONFIRM
			)))
			.isInstanceOf(AiConversationMutationRejectedException.class);
	}

	private static String actionStatus(Map<String, Object> metadata) {
		Object pendingAction = metadata.get("pendingAction");
		assertThat(pendingAction).isInstanceOf(Map.class);
		return String.valueOf(((Map<?, ?>) pendingAction).get("status"));
	}

	private static AiConversationService serviceWithBoard(
		CapturingRepository repository,
		AiConversationBoardGateway boardGateway,
		UUID... ids
	) {
		return serviceWithBoardAndRefiner(repository, boardGateway, null, ids);
	}

	private static AiConversationService serviceWithBoardAndRefiner(
		CapturingRepository repository,
		AiConversationBoardGateway boardGateway,
		AiConversationQuestionRefiner refiner,
		UUID... ids
	) {
		Queue<UUID> idQueue = new ArrayDeque<>(List.of(ids));
		return new AiConversationService(
			repository,
			CLOCK,
			() -> {
				UUID id = idQueue.poll();
				if (id == null) {
					throw new AssertionError("no deterministic id left");
				}
				return id;
			},
			null,
			null,
			AiConversationStreamPublisher.noop(),
			boardGateway,
			refiner
		);
	}

	private static AiConversationService serviceWithCurriculum(
		CapturingRepository repository,
		AiConversationCurriculumGateway curriculumGateway,
		UUID... ids
	) {
		Queue<UUID> idQueue = new ArrayDeque<>(List.of(ids));
		return new AiConversationService(
			repository,
			CLOCK,
			() -> {
				UUID id = idQueue.poll();
				if (id == null) {
					throw new AssertionError("no deterministic id left");
				}
				return id;
			},
			null,
			null,
			AiConversationStreamPublisher.noop(),
			null,
			null,
			curriculumGateway
		);
	}

	private static final class RecordingCurriculumGateway implements AiConversationCurriculumGateway {

		private int calls;
		private UUID lastUserId;
		private UUID lastTaskId;
		private String lastStatus;

		private int addCalls;
		private UUID lastAddGroupId;
		private String lastAddTitle;
		private String lastAddDescription;

		@Override
		public void completeTask(UUID authenticatedUserId, UUID taskId, String completionStatus) {
			calls++;
			lastUserId = authenticatedUserId;
			lastTaskId = taskId;
			lastStatus = completionStatus;
		}

		@Override
		public void addTaskToCurrentWeek(UUID authenticatedUserId, UUID groupId, String title, String description) {
			addCalls++;
			lastUserId = authenticatedUserId;
			lastAddGroupId = groupId;
			lastAddTitle = title;
			lastAddDescription = description;
		}
	}

	private static final class RecordingQuestionRefiner implements AiConversationQuestionRefiner {

		private final RefinedQuestionPost refined;
		private String lastInstruction;

		private RecordingQuestionRefiner(RefinedQuestionPost refined) {
			this.refined = refined;
		}

		@Override
		public RefinedQuestionPost refine(UUID authenticatedUserId, UUID groupId, String originalTitle, String originalSummary, String instruction) {
			lastInstruction = instruction;
			return refined;
		}
	}

	private static final class RecordingBoardGateway implements AiConversationBoardGateway {

		private final UUID postId;
		private final List<UUID> calls = new ArrayList<>();
		private UUID lastUserId;
		private UUID lastGroupId;
		private String lastTitle;
		private String lastContent;

		private RecordingBoardGateway(UUID postId) {
			this.postId = postId;
		}

		@Override
		public UUID shareQuestionToBoard(UUID authenticatedUserId, UUID groupId, String title, String content) {
			calls.add(groupId);
			lastUserId = authenticatedUserId;
			lastGroupId = groupId;
			lastTitle = title;
			lastContent = content;
			return postId;
		}

		private UUID lastUpdatedPostId;
		private UUID lastDeletedPostId;

		@Override
		public void updatePostOnBoard(UUID authenticatedUserId, UUID groupId, UUID postId, String title, String content) {
			lastUserId = authenticatedUserId;
			lastGroupId = groupId;
			lastUpdatedPostId = postId;
			lastTitle = title;
			lastContent = content;
		}

		@Override
		public void deletePostOnBoard(UUID authenticatedUserId, UUID groupId, UUID postId) {
			lastUserId = authenticatedUserId;
			lastGroupId = groupId;
			lastDeletedPostId = postId;
		}
	}

	private static AiConversationService service(CapturingRepository repository, UUID... ids) {
		Queue<UUID> idQueue = new ArrayDeque<>(List.of(ids));
		return new AiConversationService(
			repository,
			CLOCK,
			() -> {
				UUID id = idQueue.poll();
				if (id == null) {
					throw new AssertionError("no deterministic id left");
				}
				return id;
			}
		);
	}

	private static AiConversationService serviceWithAi(
		CapturingRepository repository,
		AiConversationAssistantResponseGenerator generator,
		LlmUsageRecorder usageRecorder,
		UUID... ids
	) {
		Queue<UUID> idQueue = new ArrayDeque<>(List.of(ids));
		return new AiConversationService(
			repository,
			CLOCK,
			() -> {
				UUID id = idQueue.poll();
				if (id == null) {
					throw new AssertionError("no deterministic id left");
				}
				return id;
			},
			generator,
			usageRecorder
		);
	}

	private static AiConversationService serviceWithStream(
		CapturingRepository repository,
		AiConversationStreamPublisher streamPublisher,
		UUID... ids
	) {
		Queue<UUID> idQueue = new ArrayDeque<>(List.of(ids));
		return new AiConversationService(
			repository,
			CLOCK,
			() -> {
				UUID id = idQueue.poll();
				if (id == null) {
					throw new AssertionError("no deterministic id left");
				}
				return id;
			},
			null,
			null,
			streamPublisher
		);
	}

	private static AiConversationService serviceWithAiAndStream(
		CapturingRepository repository,
		AiConversationAssistantResponseGenerator generator,
		LlmUsageRecorder usageRecorder,
		AiConversationStreamPublisher streamPublisher,
		UUID... ids
	) {
		Queue<UUID> idQueue = new ArrayDeque<>(List.of(ids));
		return new AiConversationService(
			repository,
			CLOCK,
			() -> {
				UUID id = idQueue.poll();
				if (id == null) {
					throw new AssertionError("no deterministic id left");
				}
				return id;
			},
			generator,
			usageRecorder,
			streamPublisher
		);
	}

	private static LlmStructuredResponse successResponse() {
		return new LlmStructuredResponse(
			LlmProvider.OPENAI,
			"gpt-test",
			"""
				{"message":"이번 주 필수 과제 하나를 줄이는 방향으로 조정해볼게요.","conversationSummary":"과제 양 조정 요청을 확인했고 다음 주 난이도 조절 후보를 제안했습니다.","nextWeekAdjustmentCandidate":{"difficulty":"LOWER"}}""",
			120,
			64,
			BigDecimal.valueOf(0.001),
			900,
			LlmUsageStatus.SUCCESS,
			null,
			Map.of("purpose", "TEAM_LEAD_CHAT", "conversationId", CONVERSATION_ID.toString()),
			"Generated AI conversation response."
		);
	}

	private static final class CapturingRepository implements AiConversationRepository {

		private boolean groupExists = true;
		private AiConversationMembershipContext membership = new AiConversationMembershipContext(
			GROUP_ID,
			MEMBER_ID,
			StudyGroupStatus.ACTIVE,
			GroupMemberPermission.MEMBER,
			GroupMemberStatus.ACTIVE
		);
		private UUID weekGroupId = GROUP_ID;
		private AiRetrospectiveReference retrospectiveReference;
		private AiConversation openTeamLeadConversation;
		private AiConversation insertedConversation;
		private boolean conversationExists = true;
		private AiConversationMessageContext messageContext = new AiConversationMessageContext(
			CONVERSATION_ID,
			GROUP_ID,
			MEMBER_ID,
			AiConversationStatus.OPEN,
			StudyGroupStatus.ACTIVE,
			GroupMemberStatus.ACTIVE
		);
		private AiConversationPromptContext promptContext = AiConversationPromptContext.empty();
		private List<AiConversationMessage> messages = List.of();
		private AiConversationMessage insertedMessage;
		private final List<AiConversationMessage> insertedMessages = new ArrayList<>();
		private int lastMessageLimit;
		private String updatedSummary;
		private AiConversationMessage messageForAction;
		private UUID updatedMetadataMessageId;
		private Map<String, Object> updatedMetadata;

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			return groupExists;
		}

		@Override
		public Optional<AiConversationMembershipContext> findMembership(UUID groupId, UUID userId) {
			return Optional.ofNullable(membership);
		}

		@Override
		public Optional<UUID> findWeekGroupId(UUID weekId) {
			return Optional.ofNullable(weekGroupId);
		}

		@Override
		public Optional<AiRetrospectiveReference> findRetrospectiveReference(UUID retrospectiveId) {
			return Optional.ofNullable(retrospectiveReference);
		}

		@Override
		public Optional<AiConversation> findOpenTeamLeadConversation(UUID groupId, UUID memberId) {
			return Optional.ofNullable(openTeamLeadConversation);
		}

		@Override
		public boolean insertConversation(AiConversation conversation) {
			insertedConversation = conversation;
			return true;
		}

		@Override
		public boolean existsConversation(UUID conversationId) {
			return conversationExists;
		}

		@Override
		public Optional<AiConversationMessageContext> findMessageContext(UUID conversationId, UUID userId) {
			return Optional.ofNullable(messageContext);
		}

		@Override
		public boolean insertMessage(AiConversationMessage message) {
			insertedMessage = message;
			insertedMessages.add(message);
			return true;
		}

		@Override
		public Optional<AiConversationMessage> findMessage(UUID messageId) {
			if (messageForAction != null && messageForAction.id().equals(messageId)) {
				return Optional.of(messageForAction);
			}
			return insertedMessages.stream().filter(message -> message.id().equals(messageId)).findFirst();
		}

		@Override
		public boolean updateMessageMetadata(UUID messageId, Map<String, Object> metadata) {
			updatedMetadataMessageId = messageId;
			updatedMetadata = metadata;
			return true;
		}

		@Override
		public List<AiConversationMessage> findMessages(UUID conversationId, AiConversationMessageCursor cursor, int limit) {
			lastMessageLimit = limit;
			return messages;
		}

		@Override
		public AiConversationPromptContext findPromptContext(AiConversationMessageContext context, int recentMessageLimit) {
			return promptContext;
		}

		@Override
		public boolean updateConversationSummary(UUID conversationId, String summary, Instant updatedAt) {
			updatedSummary = summary;
			return true;
		}
	}

	private static final class FakeAssistantGenerator implements AiConversationAssistantResponseGenerator {

		private final AiConversationAssistantResponse response;
		private final RuntimeException failure;
		private AiConversationAssistantRequest request;

		private FakeAssistantGenerator(AiConversationAssistantResponse response) {
			this.response = response;
			this.failure = null;
		}

		private FakeAssistantGenerator(RuntimeException failure) {
			this.response = null;
			this.failure = failure;
		}

		@Override
		public AiConversationAssistantResponse generate(AiConversationAssistantRequest request) {
			this.request = request;
			if (failure != null) {
				throw failure;
			}
			return response;
		}
	}

	private static final class CapturingUsageRecorder implements LlmUsageRecorder {

		private final List<LlmUsage> recorded = new ArrayList<>();

		@Override
		public void record(LlmUsage usage) {
			recorded.add(usage);
		}
	}

	private record StreamEvent(String name) {
	}

	private record FailureEvent(UUID conversationId, String errorCode) {
	}

	private static final class CapturingStreamPublisher implements AiConversationStreamPublisher {

		private final List<StreamEvent> events = new ArrayList<>();
		private final List<AiConversationMessage> messages = new ArrayList<>();
		private final List<FailureEvent> failures = new ArrayList<>();

		@Override
		public void publishUserMessageSaved(AiConversationMessage message) {
			events.add(new StreamEvent(USER_MESSAGE_SAVED_EVENT));
			messages.add(message);
		}

		@Override
		public void publishAssistantGenerationStarted(UUID conversationId) {
			events.add(new StreamEvent(ASSISTANT_GENERATION_STARTED_EVENT));
		}

		@Override
		public void publishAssistantMessageCreated(AiConversationMessage message) {
			events.add(new StreamEvent(ASSISTANT_MESSAGE_CREATED_EVENT));
			messages.add(message);
		}

		@Override
		public void publishAssistantGenerationFailed(UUID conversationId, String errorCode) {
			events.add(new StreamEvent(ASSISTANT_GENERATION_FAILED_EVENT));
			failures.add(new FailureEvent(conversationId, errorCode));
		}
	}

	private static final class ThrowingStreamPublisher implements AiConversationStreamPublisher {

		@Override
		public void publishUserMessageSaved(AiConversationMessage message) {
			throw new IllegalStateException("stream unavailable");
		}

		@Override
		public void publishAssistantGenerationStarted(UUID conversationId) {
			throw new IllegalStateException("stream unavailable");
		}

		@Override
		public void publishAssistantMessageCreated(AiConversationMessage message) {
			throw new IllegalStateException("stream unavailable");
		}

		@Override
		public void publishAssistantGenerationFailed(UUID conversationId, String errorCode) {
			throw new IllegalStateException("stream unavailable");
		}
	}
}
