package com.studypot.aistudyleader.bookmark.service;

import java.util.Objects;
import java.util.UUID;

public record ToggleBookmarkCommand(UUID authenticatedUserId, UUID groupId) {

	public ToggleBookmarkCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
	}
}
