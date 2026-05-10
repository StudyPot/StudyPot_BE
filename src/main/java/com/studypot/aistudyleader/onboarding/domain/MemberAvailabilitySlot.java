package com.studypot.aistudyleader.onboarding.domain;

import com.studypot.aistudyleader.global.domain.AuditMetadata;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.UUID;

public record MemberAvailabilitySlot(
	UUID id,
	UUID onboardingResponseId,
	UUID memberId,
	int dayOfWeek,
	LocalTime startTime,
	LocalTime endTime,
	String timezone,
	AuditMetadata auditMetadata
) {

	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

	public MemberAvailabilitySlot {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(onboardingResponseId, "onboardingResponseId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		if (dayOfWeek < 0 || dayOfWeek > 6) {
			throw new IllegalArgumentException("availabilitySlots dayOfWeek must be between 0 and 6: " + dayOfWeek);
		}
		Objects.requireNonNull(startTime, "startTime must not be null");
		Objects.requireNonNull(endTime, "endTime must not be null");
		if (!endTime.isAfter(startTime)) {
			throw new IllegalArgumentException("availabilitySlots endTime must be after startTime");
		}
		timezone = normalizeTimezone(timezone);
		Objects.requireNonNull(auditMetadata, "auditMetadata must not be null");
	}

	public static MemberAvailabilitySlot create(
		UUID id,
		UUID onboardingResponseId,
		UUID memberId,
		int dayOfWeek,
		String startTime,
		String endTime,
		String timezone,
		Instant now
	) {
		return new MemberAvailabilitySlot(
			id,
			onboardingResponseId,
			memberId,
			dayOfWeek,
			parseTime(startTime, "startTime"),
			parseTime(endTime, "endTime"),
			timezone,
			AuditMetadata.created(now)
		);
	}

	public static MemberAvailabilitySlot rehydrate(
		UUID id,
		UUID onboardingResponseId,
		UUID memberId,
		int dayOfWeek,
		LocalTime startTime,
		LocalTime endTime,
		String timezone,
		Instant createdAt,
		Instant updatedAt
	) {
		return new MemberAvailabilitySlot(
			id,
			onboardingResponseId,
			memberId,
			dayOfWeek,
			startTime,
			endTime,
			timezone,
			new AuditMetadata(createdAt, updatedAt, null)
		);
	}

	private static LocalTime parseTime(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("availabilitySlots " + fieldName + " must not be blank");
		}
		String normalized = value.strip();
		try {
			return LocalTime.parse(normalized, TIME_FORMAT);
		} catch (DateTimeParseException exception) {
			throw new IllegalArgumentException("availabilitySlots " + fieldName + " must use HH:mm format: " + normalized, exception);
		}
	}

	private static String normalizeTimezone(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("availabilitySlots timezone must not be blank");
		}
		String normalized = value.strip();
		try {
			ZoneId.of(normalized);
			return normalized;
		} catch (DateTimeException exception) {
			throw new IllegalArgumentException("availabilitySlots timezone is invalid: " + normalized, exception);
		}
	}
}
