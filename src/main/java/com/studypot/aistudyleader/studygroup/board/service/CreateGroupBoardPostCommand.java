package com.studypot.aistudyleader.studygroup.board.service;

import java.util.Objects;
import java.util.UUID;

public record CreateGroupBoardPostCommand(
	UUID authenticatedUserId,
	UUID groupId,
	UUID boardId,
	String title,
	String content,
	boolean pinned
) {

	public CreateGroupBoardPostCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(boardId, "boardId must not be null");
	}
}
