package com.studypot.aistudyleader.studygroup.board.service;

import java.util.Objects;
import java.util.UUID;

public record DeleteGroupBoardCommentCommand(UUID authenticatedUserId, UUID groupId, UUID commentId) {

	public DeleteGroupBoardCommentCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(commentId, "commentId must not be null");
	}
}
