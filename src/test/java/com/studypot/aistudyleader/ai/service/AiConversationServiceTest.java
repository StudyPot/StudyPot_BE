package com.studypot.aistudyleader.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.ai.domain.AiConversation;
import com.studypot.aistudyleader.ai.domain.AiConversationMembershipContext;
import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageContext;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageCursor;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageSenderType;
import com.studypot.aistudyleader.ai.domain.AiConversationStatus;
import com.studypot.aistudyleader.ai.domain.AiConversationType;
import com.studypot.aistudyleader.ai.domain.AiRetrospectiveReference;
import com.studypot.aistudyleader.ai.repository.AiConversationRepository;
import com.studypot.aistudyleader.global.api.CursorPageResponse;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
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
	void openRetrospectiveConversationLinksSameMemberRetrospectiveAndInfersWeek() {
		CapturingRepository repository = new CapturingRepository();
		repository.retrospectiveReference = new AiRetrospectiveReference(GROUP_ID, MEMBER_ID, WEEK_ID);
		AiConversationService service = service(repository, CONVERSATION_ID);

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
		private List<AiConversationMessage> messages = List.of();
		private AiConversationMessage insertedMessage;
		private int lastMessageLimit;

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
			return true;
		}

		@Override
		public List<AiConversationMessage> findMessages(UUID conversationId, AiConversationMessageCursor cursor, int limit) {
			lastMessageLimit = limit;
			return messages;
		}
	}
}
