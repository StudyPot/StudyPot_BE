package com.studypot.aistudyleader.curriculum.service;

import java.util.Objects;

/**
 * 다음 주차 계획 생성 입력: 다음 주차 정보 + 직전 주차 리포트 본문.
 */
public record NextWeekPlanInput(int weekNumber, String weekTitle, String sprintGoal, String reportText) {

	public NextWeekPlanInput {
		weekTitle = (weekTitle == null || weekTitle.isBlank()) ? (weekNumber + "주차") : weekTitle.strip();
		sprintGoal = sprintGoal == null ? "" : sprintGoal.strip();
		reportText = Objects.requireNonNull(reportText, "reportText must not be null").strip();
	}
}
