package com.studypot.aistudyleader.studygroup.board.service;

import java.util.Objects;
import java.util.UUID;

public record ListGroupBoardPostsQuery(
	UUID authenticatedUserId,
	UUID groupId,
	UUID boardId,
	String cursor,
	int pageSize
) {

	public ListGroupBoardPostsQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(boardId, "boardId must not be null");
		cursor = cursor == null || cursor.isBlank() ? null : cursor.strip();
		if (pageSize < 1 || pageSize > 100) {
			throw new InvalidGroupBoardRequestException("pageSize", "pageSize must be between 1 and 100.");
		}
	}
}
