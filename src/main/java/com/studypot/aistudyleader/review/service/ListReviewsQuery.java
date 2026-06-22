package com.studypot.aistudyleader.review.service;

import java.util.Objects;
import java.util.UUID;

public record ListReviewsQuery(
	UUID authenticatedUserId,
	UUID groupId
) {

	public ListReviewsQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
	}
}
