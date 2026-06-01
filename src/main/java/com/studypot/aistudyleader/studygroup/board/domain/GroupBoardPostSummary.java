package com.studypot.aistudyleader.studygroup.board.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record GroupBoardPostSummary(
	UUID id,
	UUID groupId,
	UUID boardId,
	UUID authorMemberId,
	UUID authorUserId,
	String authorDisplayName,
	String title,
	String contentPreview,
	boolean pinned,
	int commentCount,
	Instant createdAt,
	Instant updatedAt
) {

	public GroupBoardPostSummary {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(boardId, "boardId must not be null");
		Objects.requireNonNull(authorMemberId, "authorMemberId must not be null");
		title = GroupBoard.normalizeRequired(title, "title", 200);
		contentPreview = contentPreview == null ? "" : contentPreview.strip();
		if (commentCount < 0) {
			throw new IllegalArgumentException("commentCount must be non-negative.");
		}
		authorDisplayName = GroupBoard.normalizeOptional(authorDisplayName);
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

}
