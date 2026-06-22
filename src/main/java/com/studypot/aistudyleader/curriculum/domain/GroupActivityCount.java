package com.studypot.aistudyleader.curriculum.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * 그룹 활동 잔디(히트맵) 집계의 단일 행입니다.
 * 활동이 전혀 없는 멤버도 목록에 포함되도록, 그 경우 date 는 null, count 는 0 으로 내려옵니다.
 */
public record GroupActivityCount(
	UUID memberId,
	UUID userId,
	String displayName,
	String nickname,
	LocalDate date,
	int count
) {

	public GroupActivityCount {
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		if (count < 0) {
			throw new IllegalArgumentException("count must not be negative.");
		}
	}
}
