package com.studypot.aistudyleader.follow.service;

import java.util.Objects;
import java.util.UUID;

/** 팔로우 토글 결과입니다. following 은 토글 후 내가 대상 사용자를 팔로우 중인지 여부입니다. */
public record FollowToggleResult(UUID userId, boolean following) {

	public FollowToggleResult {
		Objects.requireNonNull(userId, "userId must not be null");
	}
}
