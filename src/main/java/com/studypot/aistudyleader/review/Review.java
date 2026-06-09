package com.studypot.aistudyleader.review;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Review(
	UUID id,
	UUID targetId,
	UUID authorId,
	int rating,
	String content,
	Instant createdAt,
	Instant updatedAt
) {

	public Review {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(targetId, "targetId must not be null");
		Objects.requireNonNull(authorId, "authorId must not be null");
		if (rating < 1 || rating > 5) {
			throw new IllegalArgumentException("rating must be between 1 and 5.");
		}
		content = requireText(content, "content");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	boolean writtenBy(UUID userId) {
		return authorId.equals(userId);
	}

	Review update(int rating, String content, Instant updatedAt) {
		return new Review(id, targetId, authorId, rating, content, createdAt, updatedAt);
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank.");
		}
		return value.strip();
	}
}
