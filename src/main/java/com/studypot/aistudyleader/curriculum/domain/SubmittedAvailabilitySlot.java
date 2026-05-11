package com.studypot.aistudyleader.curriculum.domain;

import java.util.Objects;

public record SubmittedAvailabilitySlot(
	int dayOfWeek,
	String startTime,
	String endTime,
	String timezone
) {

	public SubmittedAvailabilitySlot {
		if (dayOfWeek < 0 || dayOfWeek > 6) {
			throw new IllegalArgumentException("dayOfWeek must be between 0 and 6");
		}
		startTime = requireText(startTime, "startTime");
		endTime = requireText(endTime, "endTime");
		timezone = requireText(timezone, "timezone");
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return Objects.requireNonNull(value).strip();
	}
}
