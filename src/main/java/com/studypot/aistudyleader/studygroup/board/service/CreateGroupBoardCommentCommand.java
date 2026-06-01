package com.studypot.aistudyleader.studygroup.board.service;

import java.util.Objects;
import java.util.UUID;

public record CreateGroupBoardCommentCommand(UUID authenticatedUserId, UUID groupId, UUID postId, String content) {

	public CreateGroupBoardCommentCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(postId, "postId must not be null");
	}
}
