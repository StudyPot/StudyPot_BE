package com.studypot.aistudyleader.follow.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 팔로잉/팔로워 목록의 단일 사용자 항목입니다.
 * mutual 은 상대도 나를 팔로우하는지(맞팔) 여부이고, followedAt 은 팔로우 관계가 만들어진 시각입니다.
 */
public record FollowUserView(
	UUID userId,
	String nickname,
	String email,
	String bio,
	boolean mutual,
	Instant followedAt
) {

	public FollowUserView {
		Objects.requireNonNull(userId, "userId must not be null");
		Objects.requireNonNull(followedAt, "followedAt must not be null");
	}
}
