package com.studypot.aistudyleader.studygroup.board.service;

import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPostSort;

import java.util.Objects;
import java.util.UUID;

/**
 * 그룹의 모든 게시판 글을 한 번에 조회하는 쿼리입니다. (게시판 전체 조회)
 */
public record ListAllGroupBoardPostsQuery(
	UUID authenticatedUserId,
	UUID groupId,
	String cursor,
	String sort,
	String order,
	int pageSize
) {

	public ListAllGroupBoardPostsQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		cursor = cursor == null || cursor.isBlank() ? null : cursor.strip();
		if (pageSize < 1 || pageSize > 100) {
			throw new InvalidGroupBoardRequestException("pageSize", "pageSize must be between 1 and 100.");
		}
	}

	// 정렬 미지정(기본 createdAt desc) 호출용 보조 생성자.
	public ListAllGroupBoardPostsQuery(UUID authenticatedUserId, UUID groupId, String cursor, int pageSize) {
		this(authenticatedUserId, groupId, cursor, null, null, pageSize);
	}

	public GroupBoardPostSort sortOrder() {
		return GroupBoardPostSort.of(sort, order);
	}
}
