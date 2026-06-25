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
	int count,
	int todoCount,
	int postCount
) {

	public GroupActivityCount {
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		if (count < 0 || todoCount < 0 || postCount < 0) {
			throw new IllegalArgumentException("counts must not be negative.");
		}
	}

	// 합산 카운트만 아는 호출용 호환 생성자(todo=count, post=0). 분리 집계 도입 전 코드/테스트 호환.
	public GroupActivityCount(UUID memberId, UUID userId, String displayName, String nickname, LocalDate date, int count) {
		this(memberId, userId, displayName, nickname, date, count, count, 0);
	}
}
