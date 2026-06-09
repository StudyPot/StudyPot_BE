package com.studypot.aistudyleader.studygroup.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record UpdateStudyGroupCommand(
	UUID authenticatedUserId,
	UUID groupId,
	String name,
	String topic,
	List<String> detailKeywords,
	int maxMembers,
	LocalDate startsAt,
	LocalDate endsAt,
	String description
) {

	private static final int MAX_NAME_LENGTH = 120;
	private static final int MAX_TOPIC_LENGTH = 120;

	public UpdateStudyGroupCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		name = requireText(name, "study group name", MAX_NAME_LENGTH);
		topic = requireText(topic, "study group topic", MAX_TOPIC_LENGTH);
		Objects.requireNonNull(detailKeywords, "detailKeywords must not be null");
		detailKeywords = normalizeDetailKeywords(detailKeywords);
		if (maxMembers <= 0) {
			throw new IllegalArgumentException("maxMembers must be positive");
		}
		Objects.requireNonNull(startsAt, "startsAt must not be null");
		Objects.requireNonNull(endsAt, "endsAt must not be null");
		if (endsAt.isBefore(startsAt)) {
			throw new IllegalArgumentException("endsAt must be on or after startsAt");
		}
		description = blankToNull(description);
	}

	private static String requireText(String value, String fieldName, int maxLength) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		String normalized = value.strip();
		if (normalized.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " length must be <= " + maxLength + ": " + normalized.length());
		}
		return normalized;
	}

	private static List<String> normalizeDetailKeywords(List<String> values) {
		if (values.isEmpty()) {
			throw new IllegalArgumentException("detailKeywords must not be empty");
		}
		return values.stream()
			.map(value -> requireText(value, "detailKeywords", 120))
			.toList();
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}
}
