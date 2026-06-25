package com.studypot.aistudyleader.studygroup.board.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record GroupBoardComment(
	UUID id,
	UUID groupId,
	UUID postId,
	UUID authorMemberId,
	UUID authorUserId,
	String authorDisplayName,
	String content,
	Instant createdAt,
	Instant updatedAt,
	Instant deletedAt
) {

	private static final int CONTENT_MAX_LENGTH = 3_000;

	public GroupBoardComment {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(postId, "postId must not be null");
		Objects.requireNonNull(authorMemberId, "authorMemberId must not be null");
		content = GroupBoard.normalizeRequired(content, "content", CONTENT_MAX_LENGTH);
		authorDisplayName = GroupBoard.normalizeOptional(authorDisplayName);
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	public static GroupBoardComment create(UUID id, UUID groupId, UUID postId, UUID authorMemberId, String content, Instant now) {
		return create(id, groupId, postId, authorMemberId, null, null, content, now);
	}

	public static GroupBoardComment create(
		UUID id,
		UUID groupId,
		UUID postId,
		UUID authorMemberId,
		UUID authorUserId,
		String authorDisplayName,
		String content,
		Instant now
	) {
		return new GroupBoardComment(
			id,
			groupId,
			postId,
			authorMemberId,
			authorUserId,
			authorDisplayName,
			content,
			now,
			now,
			null
		);
	}

	public GroupBoardComment update(String content, Instant updatedAt) {
		return new GroupBoardComment(
			id,
			groupId,
			postId,
			authorMemberId,
			authorUserId,
			authorDisplayName,
			content,
			createdAt,
			updatedAt,
			deletedAt
		);
	}

	public boolean authoredBy(UUID memberId) {
		return authorMemberId.equals(memberId);
	}

}
