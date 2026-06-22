package com.studypot.aistudyleader.bookmark.service;

import java.util.Objects;
import java.util.UUID;

public record BookmarkToggleResult(UUID groupId, boolean bookmarked) {

	public BookmarkToggleResult {
		Objects.requireNonNull(groupId, "groupId must not be null");
	}
}
