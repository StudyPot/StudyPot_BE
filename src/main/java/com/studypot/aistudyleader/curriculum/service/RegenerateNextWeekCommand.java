package com.studypot.aistudyleader.curriculum.service;

import java.util.Objects;
import java.util.UUID;

/**
 * 직전 주차 리포트를 기반으로 다음 주차 TODO/회고 프롬프트를 재생성하는 명령입니다. (그룹장 전용)
 */
public record RegenerateNextWeekCommand(UUID authenticatedUserId, UUID groupId, UUID weekId) {

	public RegenerateNextWeekCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(weekId, "weekId must not be null");
	}
}
