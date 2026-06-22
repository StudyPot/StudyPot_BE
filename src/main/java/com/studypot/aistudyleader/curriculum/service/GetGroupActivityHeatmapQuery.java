package com.studypot.aistudyleader.curriculum.service;

import java.util.Objects;
import java.util.UUID;

public record GetGroupActivityHeatmapQuery(
	UUID authenticatedUserId,
	UUID groupId,
	int days
) {

	public static final int DEFAULT_DAYS = 28;
	public static final int MAX_DAYS = 84;

	public GetGroupActivityHeatmapQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		if (days < 1 || days > MAX_DAYS) {
			throw new IllegalArgumentException("days must be between 1 and " + MAX_DAYS + ".");
		}
	}
}
