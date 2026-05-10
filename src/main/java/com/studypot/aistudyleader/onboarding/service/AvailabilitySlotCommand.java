package com.studypot.aistudyleader.onboarding.service;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public record AvailabilitySlotCommand(
	int dayOfWeek,
	String startTime,
	String endTime,
	String timezone
) {

	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

	public AvailabilitySlotCommand {
		Objects.requireNonNull(startTime, "startTime must not be null");
		Objects.requireNonNull(endTime, "endTime must not be null");
		Objects.requireNonNull(timezone, "timezone must not be null");
	}

	public void validate() {
		if (dayOfWeek < 0 || dayOfWeek > 6) {
			throw new IllegalArgumentException("availabilitySlots dayOfWeek must be between 0 and 6: " + dayOfWeek);
		}
		LocalTime start = parseTime(startTime, "startTime");
		LocalTime end = parseTime(endTime, "endTime");
		if (!end.isAfter(start)) {
			throw new IllegalArgumentException("availabilitySlots endTime must be after startTime");
		}
		validateTimezone(timezone);
	}

	private static LocalTime parseTime(String value, String fieldName) {
		if (value.isBlank()) {
			throw new IllegalArgumentException("availabilitySlots " + fieldName + " must not be blank");
		}
		String normalized = value.strip();
		try {
			return LocalTime.parse(normalized, TIME_FORMAT);
		} catch (DateTimeParseException exception) {
			throw new IllegalArgumentException("availabilitySlots " + fieldName + " must use HH:mm format: " + normalized, exception);
		}
	}

	private static void validateTimezone(String value) {
		if (value.isBlank()) {
			throw new IllegalArgumentException("availabilitySlots timezone must not be blank");
		}
		String normalized = value.strip();
		try {
			ZoneId.of(normalized);
		} catch (DateTimeException exception) {
			throw new IllegalArgumentException("availabilitySlots timezone is invalid: " + normalized, exception);
		}
	}
}
