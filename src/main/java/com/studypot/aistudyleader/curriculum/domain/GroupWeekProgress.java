package com.studypot.aistudyleader.curriculum.domain;

import java.util.UUID;

/**
 * 그룹 커리큘럼의 주차 진행 집계입니다. progressPercent 계산에 쓰입니다.
 * total 이 0이면 진행도를 산출할 수 없습니다(커리큘럼 미생성).
 */
public record GroupWeekProgress(
	UUID groupId,
	int completedWeeks,
	int inProgressWeeks,
	int totalWeeks
) {

	/**
	 * 완료 주차 + 진행 주차*0.5 를 전체 주차로 나눈 백분율(반올림)입니다.
	 * totalWeeks 가 0이면 null 을 반환해 호출 측이 기본값으로 대체하게 합니다.
	 */
	public Integer progressPercent() {
		if (totalWeeks <= 0) {
			return null;
		}
		double ratio = (completedWeeks + inProgressWeeks * 0.5) / totalWeeks;
		return (int) Math.round(ratio * 100);
	}
}
