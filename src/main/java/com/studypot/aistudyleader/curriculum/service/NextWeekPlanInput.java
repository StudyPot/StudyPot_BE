package com.studypot.aistudyleader.curriculum.service;

import java.util.List;
import java.util.Objects;

/**
 * 다음 주차 계획 생성 입력: 다음 주차 정보 + 직전 주차 리포트 본문 + 직전 주차에 한 TODO + 멤버 회고 + 조정 제안.
 * AI는 priorTasks/memberRetrospectives/adjustmentSuggestions 를 참고해 다음 주차를 만든다. 회고가 비어 있으면 priorTasks 만으로 만든다.
 * adjustmentSuggestions 는 회고 AI 피드백과 팀장 대화에서 도출된 구조화된 '다음 주차 조정 제안'(JSON)으로, 있으면 우선 반영한다.
 */
public record NextWeekPlanInput(
	int weekNumber,
	String weekTitle,
	String sprintGoal,
	String reportText,
	List<String> priorTasks,
	List<String> memberRetrospectives,
	List<String> adjustmentSuggestions
) {

	public NextWeekPlanInput {
		if (weekNumber <= 0) {
			throw new IllegalArgumentException("weekNumber must be positive");
		}
		weekTitle = (weekTitle == null || weekTitle.isBlank()) ? (weekNumber + "주차") : weekTitle.strip();
		sprintGoal = sprintGoal == null ? "" : sprintGoal.strip();
		reportText = reportText == null ? "" : reportText.strip();
		priorTasks = List.copyOf(Objects.requireNonNull(priorTasks, "priorTasks must not be null"));
		memberRetrospectives = List.copyOf(Objects.requireNonNull(memberRetrospectives, "memberRetrospectives must not be null"));
		adjustmentSuggestions = List.copyOf(Objects.requireNonNull(adjustmentSuggestions, "adjustmentSuggestions must not be null"));
	}

	// 조정 제안 없이 호출하는 호환 생성자.
	public NextWeekPlanInput(int weekNumber, String weekTitle, String sprintGoal, String reportText, List<String> priorTasks, List<String> memberRetrospectives) {
		this(weekNumber, weekTitle, sprintGoal, reportText, priorTasks, memberRetrospectives, List.of());
	}

	// 이전주 컨텍스트 없이 호출하는 호환 생성자(수동 재생성 경로 등).
	public NextWeekPlanInput(int weekNumber, String weekTitle, String sprintGoal, String reportText) {
		this(weekNumber, weekTitle, sprintGoal, reportText, List.of(), List.of(), List.of());
	}
}
