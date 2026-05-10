package com.studypot.aistudyleader.onboarding.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AvailabilitySlotCommandTest {

	@Test
	void validateAcceptsValidSlotInput() {
		AvailabilitySlotCommand command = new AvailabilitySlotCommand(2, "20:00", "22:00", "Asia/Seoul");

		assertThatCode(command::validate).doesNotThrowAnyException();
	}

	@Test
	void validateRejectsDayOfWeekOutsideZeroToSix() {
		AvailabilitySlotCommand command = new AvailabilitySlotCommand(7, "20:00", "22:00", "Asia/Seoul");

		assertThatThrownBy(command::validate)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("availabilitySlots dayOfWeek must be between 0 and 6: 7");
	}

	@Test
	void validateRejectsMalformedTime() {
		AvailabilitySlotCommand command = new AvailabilitySlotCommand(2, "8pm", "22:00", "Asia/Seoul");

		assertThatThrownBy(command::validate)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("availabilitySlots startTime must use HH:mm format: 8pm");
	}

	@Test
	void validateRejectsEndTimeThatIsNotAfterStartTime() {
		AvailabilitySlotCommand command = new AvailabilitySlotCommand(2, "22:00", "20:00", "Asia/Seoul");

		assertThatThrownBy(command::validate)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("availabilitySlots endTime must be after startTime");
	}

	@Test
	void validateRejectsInvalidTimezone() {
		AvailabilitySlotCommand command = new AvailabilitySlotCommand(2, "20:00", "22:00", "Mars/Base");

		assertThatThrownBy(command::validate)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("availabilitySlots timezone is invalid: Mars/Base");
	}
}
