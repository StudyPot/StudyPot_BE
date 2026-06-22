package com.studypot.aistudyleader.review;

import java.util.Objects;
import java.util.UUID;

public record UpdateReviewCommand(UUID reviewId, UUID requesterId, int rating, String content) {

	public UpdateReviewCommand {
		Objects.requireNonNull(reviewId, "reviewId must not be null");
		Objects.requireNonNull(requesterId, "requesterId must not be null");
	}
}
