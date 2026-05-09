package com.studypot.aistudyleader.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.onboarding.domain.GroupOnboardingResponse;
import com.studypot.aistudyleader.onboarding.domain.OnboardingMemberContext;
import com.studypot.aistudyleader.onboarding.repository.OnboardingRepository;
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
	private static final Instant NOW = Instant.parse("2026-05-09T08:20:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
	private static final OnboardingMemberContext CONTEXT = new OnboardingMemberContext(
		GROUP_ID,
		MEMBER_ID,
		List.of("JPA", "Security")
	);

	@Test
	void saveMyDraftCreatesDraftResponseForCurrentMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.memberContext = CONTEXT;
		OnboardingService service = service(repository, RESPONSE_ID);

		GroupOnboardingResponse result = service.saveMyDraft(command());

		assertThat(repository.savedResponse).isSameAs(result);
		assertThat(result.id()).isEqualTo(RESPONSE_ID);
		assertThat(result.groupId()).isEqualTo(GROUP_ID);
		assertThat(result.memberId()).isEqualTo(MEMBER_ID);
		assertThat(result.keywordSkillLevels()).containsEntry("JPA", 2);
		assertThat(result.taskPreferences()).containsEntry("READING", 4);
	}

	@Test
	void saveMyDraftUpdatesExistingResponseWithoutCreatingDuplicateId() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.memberContext = CONTEXT;
		repository.existingResponse = existingResponse();
		OnboardingService service = service(repository, RESPONSE_ID);

		GroupOnboardingResponse result = service.saveMyDraft(command());

		assertThat(result.id()).isEqualTo(EXISTING_RESPONSE_ID);
		assertThat(repository.savedResponse.id()).isEqualTo(EXISTING_RESPONSE_ID);
	}

	@Test
	void saveMyDraftRejectsMissingGroup() {
		CapturingRepository repository = new CapturingRepository();
		OnboardingService service = service(repository, RESPONSE_ID);

		assertThatThrownBy(() -> service.saveMyDraft(command()))
			.isInstanceOf(OnboardingGroupNotFoundException.class)
			.hasMessage("study group was not found.");
	}

	@Test
	void saveMyDraftRejectsNonMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		OnboardingService service = service(repository, RESPONSE_ID);

		assertThatThrownBy(() -> service.saveMyDraft(command()))
			.isInstanceOf(OnboardingMembershipRequiredException.class)
			.hasMessage("authenticated user is not a member of this study group.");
	}

	@Test
	void saveMyDraftTranslatesInvalidScoresIntoFieldErrorException() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.memberContext = CONTEXT;
		OnboardingService service = service(repository, RESPONSE_ID);

		assertThatThrownBy(() -> service.saveMyDraft(new SaveMyOnboardingCommand(
				USER_ID,
				GROUP_ID,
				Map.of("Docker", 2),
				Map.of(),
				null
			)))
			.isInstanceOf(InvalidOnboardingRequestException.class)
			.extracting("field")
			.isEqualTo("keywordSkillLevels");
	}

	@Test
	void getMyResponseReturnsExistingResponseForCurrentMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.memberContext = CONTEXT;
		repository.existingResponse = existingResponse();
		OnboardingService service = service(repository, RESPONSE_ID);

		GroupOnboardingResponse result = service.getMyResponse(new GetMyOnboardingQuery(USER_ID, GROUP_ID));

		assertThat(result).isSameAs(repository.existingResponse);
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

	private static OnboardingService service(CapturingRepository repository, UUID nextId) {
		Supplier<UUID> idGenerator = () -> nextId;
		return new OnboardingService(repository, CLOCK, idGenerator);
	}

	private static SaveMyOnboardingCommand command() {
		return new SaveMyOnboardingCommand(
			USER_ID,
			GROUP_ID,
			Map.of("JPA", 2),
			Map.of("READING", 4),
			"실습 위주가 좋아요."
		);
	}

	private static GroupOnboardingResponse existingResponse() {
		return GroupOnboardingResponse.draft(
			EXISTING_RESPONSE_ID,
			CONTEXT,
			Map.of("Security", 3),
			Map.of("PRACTICE", 5),
			null,
			NOW
		);
	}

	private static final class CapturingRepository implements OnboardingRepository {

		private boolean groupExists;
		private OnboardingMemberContext memberContext;
		private GroupOnboardingResponse existingResponse;
		private GroupOnboardingResponse savedResponse;

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			return groupExists;
		}

		@Override
		public Optional<OnboardingMemberContext> findMemberContext(UUID groupId, UUID userId) {
			return Optional.ofNullable(memberContext);
		}

		@Override
		public Optional<GroupOnboardingResponse> findResponseByMemberId(UUID memberId) {
			return Optional.ofNullable(existingResponse);
		}

		@Override
		public void saveDraft(GroupOnboardingResponse response) {
			this.savedResponse = response;
			this.existingResponse = response;
		}
	}
}
