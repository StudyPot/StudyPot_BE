package com.studypot.aistudyleader.report.service;

import java.util.UUID;

/**
 * 주차가 끝나기 전이라도(예: 전원 회고 완료) 해당 주차의 학습 리포트를 즉시 생성·게시하기 위한 포트.
 * 구현은 멱등(제목 'N주차 학습 리포트')이며 LLM 미구성 시 무시한다.
 * 주의: 이 트리거는 리포트만 게시하고 '다음 주차'는 생성하지 않는다(다음 주차는 주차 마감 후 스케줄러가 처리).
 */
public interface WeeklyReportTrigger {

	void generateForWeekImmediately(UUID weekId);
}
