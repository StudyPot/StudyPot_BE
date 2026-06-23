package com.studypot.aistudyleader.retrospective.service;

import java.util.Objects;
import java.util.UUID;

/** 그룹 내 내 회고 주차별 개요(잠금/답변/질문) 조회 쿼리입니다. */
public record GetRetrospectiveOverviewQuery(UUID authenticatedUserId, UUID groupId) {

	public GetRetrospectiveOverviewQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
	}
}
