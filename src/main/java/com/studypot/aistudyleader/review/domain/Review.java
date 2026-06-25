package com.studypot.aistudyleader.review.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Review(
	UUID id,
	UUID groupId,
	UUID memberId,
	UUID userId,
	String displayName,
	int rating,
	String content,
	Instant createdAt,
	Instant updatedAt
) {

	public static final int MIN_RATING = 1;
	public static final int MAX_RATING = 5;

	public Review {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (rating < MIN_RATING || rating > MAX_RATING) {
			throw new IllegalArgumentException("rating must be between " + MIN_RATING + " and " + MAX_RATING + ".");
		}
		displayName = normalizeNullableText(displayName);
		content = normalizeNullableText(content);
	}

	public static Review create(
		UUID id,
		UUID groupId,
		UUID memberId,
		UUID userId,
		String displayName,
		int rating,
		String content,
		Instant now
	) {
		return new Review(id, groupId, memberId, userId, displayName, rating, content, now, now);
	}

	private static String normalizeNullableText(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}
}
