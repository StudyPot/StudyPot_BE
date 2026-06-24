package com.studypot.aistudyleader.studygroup.board.service;

import java.util.Objects;
import java.util.UUID;

public record CreateGroupBoardPostCommand(
	UUID authenticatedUserId,
	UUID groupId,
	UUID boardId,
	String title,
	String content,
	boolean pinned,
	String authorDisplayNameOverride
) {

	public CreateGroupBoardPostCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(boardId, "boardId must not be null");
	}

	// 작성자 표시명 오버라이드 없이 호출하는 호환 생성자(일반 게시글 작성).
	public CreateGroupBoardPostCommand(
		UUID authenticatedUserId,
		UUID groupId,
		UUID boardId,
		String title,
		String content,
		boolean pinned
	) {
		this(authenticatedUserId, groupId, boardId, title, content, pinned, null);
	}
}
