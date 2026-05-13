package com.studypot.aistudyleader.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.ai.domain.AiConversation;
import com.studypot.aistudyleader.ai.domain.AiConversationMembershipContext;
import com.studypot.aistudyleader.ai.domain.AiConversationStatus;
import com.studypot.aistudyleader.ai.domain.AiConversationType;
import com.studypot.aistudyleader.ai.domain.AiRetrospectiveReference;
import com.studypot.aistudyleader.ai.repository.AiConversationRepository;
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
	}
}
