package com.studypot.aistudyleader.review.service;

import java.util.Objects;
import java.util.UUID;

public record GetReviewStatsQuery(
	UUID authenticatedUserId,
	UUID groupId
) {

	public GetReviewStatsQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
	}
}
