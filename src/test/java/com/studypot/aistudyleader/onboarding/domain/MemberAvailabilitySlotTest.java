package com.studypot.aistudyleader.onboarding.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MemberAvailabilitySlotTest {

	private static final UUID SLOT_ID = UUID.fromString("018f0000-0000-7000-8000-000000003101");
	private static final UUID RESPONSE_ID = UUID.fromString("018f0000-0000-7000-8000-000000003102");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000003103");
	private static final Instant NOW = Instant.parse("2026-05-10T01:10:00Z");

	@Test
	void createBuildsAvailabilitySlotWithValidatedWindow() {
		MemberAvailabilitySlot slot = MemberAvailabilitySlot.create(
			SLOT_ID,
			RESPONSE_ID,
			MEMBER_ID,
			2,
			"20:00",
			"22:00",
			" Asia/Seoul ",
			NOW
		);

		assertThat(slot.id()).isEqualTo(SLOT_ID);
		assertThat(slot.onboardingResponseId()).isEqualTo(RESPONSE_ID);
		assertThat(slot.memberId()).isEqualTo(MEMBER_ID);
		assertThat(slot.dayOfWeek()).isEqualTo(2);
		assertThat(slot.startTime()).isEqualTo(LocalTime.of(20, 0));
		assertThat(slot.endTime()).isEqualTo(LocalTime.of(22, 0));
		assertThat(slot.timezone()).isEqualTo("Asia/Seoul");
		assertThat(slot.auditMetadata().createdAt()).isEqualTo(NOW);
	}

	@Test
	void createRejectsDayOfWeekOutsideZeroToSix() {
		assertThatThrownBy(() -> MemberAvailabilitySlot.create(
				SLOT_ID,
				RESPONSE_ID,
				MEMBER_ID,
				7,
				"20:00",
				"22:00",
				"Asia/Seoul",
				NOW
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("availabilitySlots dayOfWeek must be between 0 and 6: 7");
	}

	@Test
	void createRejectsMalformedTime() {
		assertThatThrownBy(() -> MemberAvailabilitySlot.create(
				SLOT_ID,
				RESPONSE_ID,
				MEMBER_ID,
				1,
				"8pm",
				"22:00",
				"Asia/Seoul",
				NOW
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("availabilitySlots startTime must use HH:mm format: 8pm");
	}

	@Test
	void createRejectsEndTimeThatIsNotAfterStartTime() {
		assertThatThrownBy(() -> MemberAvailabilitySlot.create(
				SLOT_ID,
				RESPONSE_ID,
				MEMBER_ID,
				1,
				"22:00",
				"20:00",
				"Asia/Seoul",
				NOW
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("availabilitySlots endTime must be after startTime");
	}

	@Test
	void createRejectsInvalidTimezone() {
		assertThatThrownBy(() -> MemberAvailabilitySlot.create(
				SLOT_ID,
				RESPONSE_ID,
				MEMBER_ID,
				1,
				"20:00",
				"22:00",
				"Mars/Base",
				NOW
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("availabilitySlots timezone is invalid: Mars/Base");
	}
}
