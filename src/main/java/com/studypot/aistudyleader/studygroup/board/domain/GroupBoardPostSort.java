package com.studypot.aistudyleader.studygroup.board.domain;

/**
 * 게시글 목록 정렬 옵션입니다. 고정글(is_pinned)은 항상 최상단 우선이며 그 다음 기준으로 정렬합니다.
 * keyset 커서는 기본 정렬(createdAt desc)에서만 안정적이므로, 그 외 정렬은 단일 페이지(nextCursor 없음)로 응답합니다.
 */
public enum GroupBoardPostSort {

	CREATED_AT_DESC("p.created_at desc, p.id desc", true),
	CREATED_AT_ASC("p.created_at asc, p.id asc", false),
	COMMENT_COUNT_DESC("comment_count desc, p.created_at desc, p.id desc", false),
	COMMENT_COUNT_ASC("comment_count asc, p.created_at asc, p.id asc", false);

	private final String orderByTail;
	private final boolean keysetCursorSupported;

	GroupBoardPostSort(String orderByTail, boolean keysetCursorSupported) {
		this.orderByTail = orderByTail;
		this.keysetCursorSupported = keysetCursorSupported;
	}

	/** sort=createdAt|commentCount, order=asc|desc 를 안전하게 매핑(미지정/오타는 기본값). */
	public static GroupBoardPostSort of(String sort, String order) {
		boolean asc = "asc".equalsIgnoreCase(order);
		if ("commentCount".equalsIgnoreCase(sort)) {
			return asc ? COMMENT_COUNT_ASC : COMMENT_COUNT_DESC;
		}
		return asc ? CREATED_AT_ASC : CREATED_AT_DESC;
	}

	/** 고정글 우선 + 선택 정렬을 합친 ORDER BY 절. */
	public String orderByClause() {
		return "order by p.is_pinned desc, " + orderByTail;
	}

	public boolean keysetCursorSupported() {
		return keysetCursorSupported;
	}
}
