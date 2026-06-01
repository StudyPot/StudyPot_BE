package com.studypot.aistudyleader.studygroup.board.service;

import java.util.Objects;
import java.util.UUID;

public record UpdateGroupBoardCommentCommand(UUID authenticatedUserId, UUID groupId, UUID commentId, String content) {

	public UpdateGroupBoardCommentCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(commentId, "commentId must not be null");
		content = content == null ? null : content.strip();
		if (content == null || content.isBlank()) {
			throw new InvalidGroupBoardRequestException("content", "content must not be blank.");
		}
	}
}
