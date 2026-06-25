package com.studypot.aistudyleader.retrospective.service;

import java.util.Objects;
import java.util.UUID;

/**
 * 그룹 내 내가 작성한 모든 주차의 회고를 조회하는 쿼리입니다. (리뷰=회고 조회)
 */
public record ListMyRetrospectivesQuery(
	UUID authenticatedUserId,
	UUID groupId
) {

	public ListMyRetrospectivesQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
	}
}
