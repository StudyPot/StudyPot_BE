package com.studypot.aistudyleader.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.onboarding.domain.GroupOnboardingResponse;
import com.studypot.aistudyleader.onboarding.domain.GroupOnboardingStatus;
import com.studypot.aistudyleader.onboarding.domain.MemberAvailabilitySlot;
import com.studypot.aistudyleader.onboarding.domain.OnboardingMemberContext;
import com.studypot.aistudyleader.onboarding.repository.OnboardingRepository;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class OnboardingServiceTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000003021");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000003022");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000003023");
	private static final UUID RESPONSE_ID = UUID.fromString("018f0000-0000-7000-8000-000000003024");
	private static final UUID EXISTING_RESPONSE_ID = UUID.fromString("018f0000-0000-7000-8000-000000003025");
	private static final UUID SLOT_ID = UUID.fromString("018f0000-0000-7000-8000-000000003026");
	private static final UUID REPLACEMENT_SLOT_ID = UUID.fromString("018f0000-0000-7000-8000-000000003027");
	private static final Instant NOW = Instant.parse("2026-05-09T08:20:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
	private static final OnboardingMemberContext CONTEXT = new OnboardingMemberContext(
		GROUP_ID,
		MEMBER_ID,
		List.of("JPA", "Security")
	);

	@Test
	void submitMyOnboardingCreatesSubmittedResponseForCurrentMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.memberContext = CONTEXT;
		OnboardingService service = service(repository, RESPONSE_ID);

		GroupOnboardingResponse result = service.submitMyOnboarding(command());

		assertThat(repository.savedResponse.id()).isEqualTo(RESPONSE_ID);
		assertThat(repository.submittedResponse).isSameAs(result);
		assertThat(result.groupId()).isEqualTo(GROUP_ID);
		assertThat(result.memberId()).isEqualTo(MEMBER_ID);
		assertThat(result.status()).isEqualTo(GroupOnboardingStatus.SUBMITTED);
		assertThat(result.submittedAt()).contains(NOW);
		assertThat(repository.activatedMemberId).isEqualTo(MEMBER_ID);
		assertThat(repository.activatedAt).isEqualTo(NOW);
	}

	@Test
	void submitMyOnboardingMapsOverallSkillLevelToInternalKeywordScores() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.memberContext = CONTEXT;
		OnboardingService service = service(repository, RESPONSE_ID);

		GroupOnboardingResponse result = service.submitMyOnboarding(command());

		assertThat(result.skillLevel()).isEqualTo(3);
		assertThat(result.keywordSkillLevels())
			.containsExactlyInAnyOrderEntriesOf(Map.of("JPA", 3, "Security", 3));
		assertThat(result.taskPreferences()).isEmpty();
	}

	@Test
	void submitMyOnboardingStoresAvailabilitySlotsForCurrentMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.memberContext = CONTEXT;
		OnboardingService service = service(repository, RESPONSE_ID, SLOT_ID);

		GroupOnboardingResponse result = service.submitMyOnboarding(commandWithSlots(
			new AvailabilitySlotCommand(2, "20:00", "22:00", "Asia/Seoul")
		));

		assertThat(result.availabilitySlots()).hasSize(1);
		MemberAvailabilitySlot slot = result.availabilitySlots().getFirst();
		assertThat(slot.id()).isEqualTo(SLOT_ID);
		assertThat(slot.onboardingResponseId()).isEqualTo(RESPONSE_ID);
		assertThat(slot.memberId()).isEqualTo(MEMBER_ID);
		assertThat(slot.dayOfWeek()).isEqualTo(2);
		assertThat(slot.timezone()).isEqualTo("Asia/Seoul");
	}

	@Test
	void submitMyOnboardingUpdatesExistingDraftWithoutCreatingDuplicateId() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.memberContext = CONTEXT;
		repository.existingResponse = existingDraft().withAvailabilitySlots(List.of(slot(SLOT_ID, EXISTING_RESPONSE_ID)));
		OnboardingService service = service(repository, REPLACEMENT_SLOT_ID);

		GroupOnboardingResponse result = service.submitMyOnboarding(commandWithSlots(
			new AvailabilitySlotCommand(4, "21:00", "23:00", "Asia/Seoul")
		));

		assertThat(result.id()).isEqualTo(EXISTING_RESPONSE_ID);
		assertThat(result.availabilitySlots()).extracting(MemberAvailabilitySlot::id)
			.containsExactly(REPLACEMENT_SLOT_ID);
		assertThat(result.availabilitySlots()).extracting(MemberAvailabilitySlot::dayOfWeek)
			.containsExactly(4);
	}

	@Test
	void submitMyOnboardingRejectsAlreadySubmittedResponse() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.memberContext = CONTEXT;
		repository.existingResponse = existingDraft().submit(NOW);
		OnboardingService service = service(repository, REPLACEMENT_SLOT_ID);

		assertThatThrownBy(() -> service.submitMyOnboarding(command()))
			.isInstanceOf(OnboardingAlreadySubmittedException.class)
			.hasMessage("onboarding response was already submitted.");
		assertThat(repository.savedResponse).isNull();
		assertThat(repository.submittedResponse).isNull();
	}

	@Test
	void submitMyOnboardingRejectsMissingGroup() {
		CapturingRepository repository = new CapturingRepository();
		OnboardingService service = service(repository, RESPONSE_ID);

		assertThatThrownBy(() -> service.submitMyOnboarding(command()))
			.isInstanceOf(OnboardingGroupNotFoundException.class)
			.hasMessage("study group was not found.");
	}

	@Test
	void submitMyOnboardingRejectsNonMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		OnboardingService service = service(repository, RESPONSE_ID);

		assertThatThrownBy(() -> service.submitMyOnboarding(command()))
			.isInstanceOf(OnboardingMembershipRequiredException.class)
			.hasMessage("authenticated user is not a member of this study group.");
	}

	@Test
	void submitMyOnboardingTranslatesInvalidSkillLevelIntoFieldErrorException() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.memberContext = CONTEXT;
		OnboardingService service = service(repository, RESPONSE_ID);

		assertThatThrownBy(() -> service.submitMyOnboarding(new SubmitMyOnboardingCommand(
				USER_ID,
				GROUP_ID,
				0,
				null,
				List.of()
			)))
			.isInstanceOf(InvalidOnboardingRequestException.class)
			.extracting("field")
			.isEqualTo("skillLevel");
	}

	@Test
	void submitMyOnboardingTranslatesInvalidAvailabilitySlotIntoFieldErrorException() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.memberContext = CONTEXT;
		OnboardingService service = service(repository, RESPONSE_ID, SLOT_ID);

		assertThatThrownBy(() -> service.submitMyOnboarding(commandWithSlots(
				new AvailabilitySlotCommand(1, "22:00", "20:00", "Asia/Seoul")
			)))
			.isInstanceOf(InvalidOnboardingRequestException.class)
			.extracting("field")
			.isEqualTo("availabilitySlots");
	}

	@Test
	void getMyResponseReturnsExistingResponseForCurrentMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.memberContext = CONTEXT;
		repository.existingResponse = existingDraft().withAvailabilitySlots(List.of(slot(SLOT_ID, EXISTING_RESPONSE_ID)));
		OnboardingService service = service(repository, RESPONSE_ID);

		GroupOnboardingResponse result = service.getMyResponse(new GetMyOnboardingQuery(USER_ID, GROUP_ID));

		assertThat(result).isSameAs(repository.existingResponse);
		assertThat(result.availabilitySlots()).hasSize(1);
	}

	@Test
	void getMyResponseRejectsWhenResponseDoesNotExist() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.memberContext = CONTEXT;
		OnboardingService service = service(repository, RESPONSE_ID);

		assertThatThrownBy(() -> service.getMyResponse(new GetMyOnboardingQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(OnboardingResponseNotFoundException.class)
			.hasMessage("onboarding response was not found.");
	}

	@Test
	void submitMyOnboardingSkipsActivationForAlreadyActiveMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.memberContext = new OnboardingMemberContext(GROUP_ID, MEMBER_ID, GroupMemberStatus.ACTIVE, List.of("JPA", "Security"));
		OnboardingService service = service(repository, RESPONSE_ID);

		GroupOnboardingResponse result = service.submitMyOnboarding(command());

		assertThat(result.status()).isEqualTo(GroupOnboardingStatus.SUBMITTED);
		assertThat(repository.activatedMemberId).isNull();
	}

	@Test
	void submitMyOnboardingRejectsWhenPendingMemberCannotBeActivated() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.memberContext = CONTEXT;
		repository.activateResult = false;
		OnboardingService service = service(repository, RESPONSE_ID);

		assertThatThrownBy(() -> service.submitMyOnboarding(command()))
			.isInstanceOf(OnboardingMembershipRequiredException.class)
			.hasMessage("current group membership is required.");
	}

	private static OnboardingService service(CapturingRepository repository, UUID... nextIds) {
		Supplier<UUID> idGenerator = new DeterministicIds(nextIds);
		return new OnboardingService(repository, CLOCK, idGenerator);
	}

	private static SubmitMyOnboardingCommand command() {
		return new SubmitMyOnboardingCommand(
			USER_ID,
			GROUP_ID,
			3,
			"JPA는 처음이고 실습 위주가 좋아요.",
			List.of()
		);
	}

	private static SubmitMyOnboardingCommand commandWithSlots(AvailabilitySlotCommand... slots) {
		return new SubmitMyOnboardingCommand(
			USER_ID,
			GROUP_ID,
			3,
			"JPA는 처음이고 실습 위주가 좋아요.",
			List.of(slots)
		);
	}

	private static GroupOnboardingResponse existingDraft() {
		return GroupOnboardingResponse.draft(
			EXISTING_RESPONSE_ID,
			CONTEXT,
			Map.of("Security", 2, "JPA", 2),
			Map.of(),
			null,
			NOW
		);
	}

	private static MemberAvailabilitySlot slot(UUID slotId, UUID responseId) {
		return MemberAvailabilitySlot.create(
			slotId,
			responseId,
			MEMBER_ID,
			2,
			"20:00",
			"22:00",
			"Asia/Seoul",
			NOW
		);
	}

	private static final class DeterministicIds implements Supplier<UUID> {

		private final List<UUID> ids;
		private int index;

		private DeterministicIds(UUID... ids) {
			this.ids = List.of(ids);
		}

		@Override
		public UUID get() {
			if (index >= ids.size()) {
				throw new AssertionError("no deterministic id left");
			}
			UUID next = ids.get(index);
			index++;
			return next;
		}
	}

	private static final class CapturingRepository implements OnboardingRepository {

		private boolean groupExists;
		private OnboardingMemberContext memberContext;
		private GroupOnboardingResponse existingResponse;
		private GroupOnboardingResponse savedResponse;
		private GroupOnboardingResponse submittedResponse;
		private UUID activatedMemberId;
		private Instant activatedAt;
		private boolean activateResult = true;

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			requireExpectedGroupId(groupId);
			return groupExists;
		}

		@Override
		public Optional<OnboardingMemberContext> findMemberContext(UUID groupId, UUID userId) {
			requireExpectedGroupId(groupId);
			if (!USER_ID.equals(userId)) {
				throw new AssertionError("unexpected userId: " + userId);
			}
			return Optional.ofNullable(memberContext);
		}

		@Override
		public Optional<GroupOnboardingResponse> findResponseByMemberId(UUID memberId) {
			if (!MEMBER_ID.equals(memberId)) {
				throw new AssertionError("unexpected memberId: " + memberId);
			}
			return Optional.ofNullable(existingResponse);
		}

		@Override
		public GroupOnboardingResponse saveDraft(GroupOnboardingResponse response) {
			this.savedResponse = response;
			this.existingResponse = response;
			return response;
		}

		@Override
		public GroupOnboardingResponse submit(GroupOnboardingResponse response) {
			this.submittedResponse = response;
			this.existingResponse = response;
			return response;
		}

		@Override
		public boolean activatePendingMember(UUID memberId, Instant activatedAt) {
			this.activatedMemberId = memberId;
			this.activatedAt = activatedAt;
			return activateResult;
		}

		private static void requireExpectedGroupId(UUID groupId) {
			if (!GROUP_ID.equals(groupId)) {
				throw new AssertionError("unexpected groupId: " + groupId);
			}
		}
	}
}
