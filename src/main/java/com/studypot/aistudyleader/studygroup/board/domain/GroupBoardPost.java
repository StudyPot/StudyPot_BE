package com.studypot.aistudyleader.studygroup.board.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record GroupBoardPost(
	UUID id,
	UUID groupId,
	UUID boardId,
	UUID authorMemberId,
	UUID authorUserId,
	String authorDisplayName,
	String authorDisplayNameOverride,
	String title,
	String content,
	boolean pinned,
	Instant createdAt,
	Instant updatedAt,
	Instant deletedAt
) {

	private static final int TITLE_MAX_LENGTH = 200;
	private static final int CONTENT_MAX_LENGTH = 10_000;

	public GroupBoardPost {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(boardId, "boardId must not be null");
		Objects.requireNonNull(authorMemberId, "authorMemberId must not be null");
		title = GroupBoard.normalizeRequired(title, "title", TITLE_MAX_LENGTH);
		content = GroupBoard.normalizeRequired(content, "content", CONTENT_MAX_LENGTH);
		authorDisplayName = GroupBoard.normalizeOptional(authorDisplayName);
		authorDisplayNameOverride = GroupBoard.normalizeOptional(authorDisplayNameOverride);
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	public static GroupBoardPost create(
		UUID id,
		UUID groupId,
		UUID boardId,
		UUID authorMemberId,
		String title,
		String content,
		boolean pinned,
		Instant now
	) {
		return create(id, groupId, boardId, authorMemberId, null, null, title, content, pinned, now);
	}

	public static GroupBoardPost create(
		UUID id,
		UUID groupId,
		UUID boardId,
		UUID authorMemberId,
		UUID authorUserId,
		String authorDisplayName,
		String title,
		String content,
		boolean pinned,
		Instant now
	) {
		return new GroupBoardPost(
			id,
			groupId,
			boardId,
			authorMemberId,
			authorUserId,
			authorDisplayName,
			null,
			title,
			content,
			pinned,
			now,
			now,
			null
		);
	}

	/**
	 * 작성자 표시명 오버라이드를 적용한 사본을 반환한다(예: AI 팀장이 올린 글을 'AI 팀장' 명의로 표시).
	 * authorMemberId(소유/권한)는 유지하고 표시명만 바꾼다.
	 */
	public GroupBoardPost withAuthorDisplayNameOverride(String authorDisplayNameOverride) {
		return new GroupBoardPost(
			id,
			groupId,
			boardId,
			authorMemberId,
			authorUserId,
			authorDisplayName,
			authorDisplayNameOverride,
			title,
			content,
			pinned,
			createdAt,
			updatedAt,
			deletedAt
		);
	}

	public GroupBoardPost update(String title, String content, Boolean pinned, Instant updatedAt) {
		return new GroupBoardPost(
			id,
			groupId,
			boardId,
			authorMemberId,
			authorUserId,
			authorDisplayName,
			authorDisplayNameOverride,
			title == null ? this.title : title,
			content == null ? this.content : content,
			pinned == null ? this.pinned : pinned,
			createdAt,
			updatedAt,
			deletedAt
		);
	}

	public boolean authoredBy(UUID memberId) {
		return authorMemberId.equals(memberId);
	}

}
