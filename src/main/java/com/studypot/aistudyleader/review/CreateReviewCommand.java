package com.studypot.aistudyleader.review;

import java.util.Objects;
import java.util.UUID;

public record CreateReviewCommand(UUID targetId, UUID authorId, int rating, String content) {

	public CreateReviewCommand {
		Objects.requireNonNull(targetId, "targetId must not be null");
		Objects.requireNonNull(authorId, "authorId must not be null");
		if (rating < 1 || rating > 5) {
			throw new IllegalArgumentException("rating must be between 1 and 5.");
		}
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("content must not be blank.");
		}
		content = content.strip();
	}
}
