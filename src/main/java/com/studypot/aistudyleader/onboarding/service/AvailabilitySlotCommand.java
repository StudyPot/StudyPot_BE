package com.studypot.aistudyleader.onboarding.service;

import java.util.Objects;

public record AvailabilitySlotCommand(
	int dayOfWeek,
	String startTime,
	String endTime,
	String timezone
) {

	public AvailabilitySlotCommand {
		Objects.requireNonNull(startTime, "startTime must not be null");
		Objects.requireNonNull(endTime, "endTime must not be null");
		Objects.requireNonNull(timezone, "timezone must not be null");
	}
}
