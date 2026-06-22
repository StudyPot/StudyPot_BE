package com.studypot.aistudyleader.bookmark.service;

import java.util.Objects;
import java.util.UUID;

public record ListMyBookmarksQuery(UUID authenticatedUserId) {

	public ListMyBookmarksQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
	}
}
