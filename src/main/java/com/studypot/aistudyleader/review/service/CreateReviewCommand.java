package com.studypot.aistudyleader.review.service;

import java.util.Objects;
import java.util.UUID;

public record CreateReviewCommand(
	UUID authenticatedUserId,
	UUID groupId,
	int rating,
	String content
) {

	public CreateReviewCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
	}
}
