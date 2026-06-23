package com.studypot.aistudyleader.curriculum.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * 리포트 기반 재생성 대상이 되는 '다음 주차'의 경량 식별 정보입니다.
 */
public record NextWeekTarget(UUID weekId, int weekNumber, String title, String sprintGoal) {

	public NextWeekTarget {
		Objects.requireNonNull(weekId, "weekId must not be null");
		if (weekNumber <= 0) {
			throw new IllegalArgumentException("weekNumber must be positive");
		}
	}
}
