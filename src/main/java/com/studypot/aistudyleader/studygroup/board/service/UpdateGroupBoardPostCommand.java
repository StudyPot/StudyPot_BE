package com.studypot.aistudyleader.studygroup.board.service;

import java.util.Objects;
import java.util.UUID;

public record UpdateGroupBoardPostCommand(
	UUID authenticatedUserId,
	UUID groupId,
	UUID postId,
	String title,
	String content,
	Boolean pinned
) {

	public UpdateGroupBoardPostCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(postId, "postId must not be null");
		title = title == null ? null : title.strip();
		content = content == null ? null : content.strip();
		if (title == null && content == null && pinned == null) {
			throw new InvalidGroupBoardRequestException("request", "at least one field must be provided.");
		}
	}

	public boolean changesContent() {
		return title != null || content != null;
	}
}
