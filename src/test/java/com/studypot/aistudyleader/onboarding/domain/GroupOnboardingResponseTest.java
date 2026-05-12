package com.studypot.aistudyleader.onboarding.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GroupOnboardingResponseTest {

	private static final UUID RESPONSE_ID = UUID.fromString("018f0000-0000-7000-8000-000000003001");
	private static final UUID SLOT_ID = UUID.fromString("018f0000-0000-7000-8000-000000003004");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000003002");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000003003");
	private static final Instant NOW = Instant.parse("2026-05-09T08:10:00Z");
	private static final OnboardingMemberContext CONTEXT = new OnboardingMemberContext(
		GROUP_ID,
		MEMBER_ID,
		List.of("JPA", "Security")
	);

	@Test
	void draftCreatesDraftResponseWithValidatedScores() {
		GroupOnboardingResponse response = GroupOnboardingResponse.draft(
			RESPONSE_ID,
			CONTEXT,
			Map.of("JPA", 2, "Security", 4),
			Map.of("READING", 3, "PRACTICE", 5),
			" 실습 위주가 좋아요. ",
			NOW
		);

		assertThat(response.id()).isEqualTo(RESPONSE_ID);
		assertThat(response.groupId()).isEqualTo(GROUP_ID);
		assertThat(response.memberId()).isEqualTo(MEMBER_ID);
		assertThat(response.keywordSkillLevels()).containsEntry("JPA", 2).containsEntry("Security", 4);
		assertThat(response.taskPreferences()).containsEntry("READING", 3).containsEntry("PRACTICE", 5);
		assertThat(response.additionalNote()).contains("실습 위주가 좋아요.");
		assertThat(response.status()).isEqualTo(GroupOnboardingStatus.DRAFT);
		assertThat(response.submittedAt()).isEmpty();
		assertThat(response.auditMetadata().createdAt()).isEqualTo(NOW);
	}

	@Test
	void withAvailabilitySlotsAttachesSlotsForSameResponseAndMember() {
		MemberAvailabilitySlot slot = slot(RESPONSE_ID, MEMBER_ID);

		GroupOnboardingResponse response = draft().withAvailabilitySlots(List.of(slot));

		assertThat(response.availabilitySlots()).containsExactly(slot);
	}

	@Test
	void withAvailabilitySlotsRejectsSlotForDifferentResponse() {
		MemberAvailabilitySlot slot = slot(UUID.fromString("018f0000-0000-7000-8000-000000003099"), MEMBER_ID);

		assertThatThrownBy(() -> draft().withAvailabilitySlots(List.of(slot)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("availabilitySlots must belong to onboarding response");
	}

	@Test
	void draftAllowsPartialMapsForTemporarySave() {
		GroupOnboardingResponse response = GroupOnboardingResponse.draft(
			RESPONSE_ID,
			CONTEXT,
			Map.of("JPA", 2),
			Map.of(),
			null,
			NOW
		);

		assertThat(response.keywordSkillLevels()).containsOnly(Map.entry("JPA", 2));
		assertThat(response.taskPreferences()).isEmpty();
	}

	@Test
	void submitMarksDraftAsSubmittedAndStoresSubmittedAt() {
		Instant submittedAt = NOW.plusSeconds(120);

		GroupOnboardingResponse submitted = draft().submit(submittedAt);

		assertThat(submitted.status()).isEqualTo(GroupOnboardingStatus.SUBMITTED);
		assertThat(submitted.submittedAt()).contains(submittedAt);
		assertThat(submitted.auditMetadata().updatedAt()).isEqualTo(submittedAt);
	}

	@Test
	void draftRejectsUnknownKeyword() {
		assertThatThrownBy(() -> GroupOnboardingResponse.draft(
				RESPONSE_ID,
				CONTEXT,
				Map.of("Docker", 3),
				Map.of(),
				null,
				NOW
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("keywordSkillLevels contains unknown group keyword: Docker");
	}

	@Test
	void draftRejectsUnknownTaskPreferenceType() {
		assertThatThrownBy(() -> GroupOnboardingResponse.draft(
				RESPONSE_ID,
				CONTEXT,
				Map.of(),
				Map.of("PAIRING", 3),
				null,
				NOW
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("taskPreferences contains unknown task type: PAIRING");
	}

	@Test
	void draftRejectsScoreOutsideOneToFive() {
		assertThatThrownBy(() -> GroupOnboardingResponse.draft(
				RESPONSE_ID,
				CONTEXT,
				Map.of("JPA", 0),
				Map.of(),
				null,
				NOW
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("keywordSkillLevels score must be between 1 and 5: JPA=0");
	}

	@Test
	void draftRejectsScoreAboveFive() {
		assertThatThrownBy(() -> GroupOnboardingResponse.draft(
				RESPONSE_ID,
				CONTEXT,
				Map.of("JPA", 6),
				Map.of(),
				null,
				NOW
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("keywordSkillLevels score must be between 1 and 5: JPA=6");
	}

	private static GroupOnboardingResponse draft() {
		return GroupOnboardingResponse.draft(
			RESPONSE_ID,
			CONTEXT,
			Map.of("JPA", 2),
			Map.of("READING", 4),
			null,
			NOW
		);
	}

	private static MemberAvailabilitySlot slot(UUID responseId, UUID memberId) {
		return MemberAvailabilitySlot.create(
			SLOT_ID,
			responseId,
			memberId,
			2,
			"20:00",
			"22:00",
			"Asia/Seoul",
			NOW
		);
	}
}
