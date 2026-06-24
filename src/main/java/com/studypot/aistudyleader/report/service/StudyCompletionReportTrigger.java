package com.studypot.aistudyleader.report.service;

import java.util.UUID;

/**
 * 스터디 완료 직후 수료 리포트 생성을 즉시 트리거하기 위한 포트.
 * 주기 스케줄러(WeeklyReportScheduler)의 15분 폴링을 기다리지 않고 완료 시점에 바로 생성하도록 한다.
 * 구현은 멱등(제목 '수료 리포트' 기준)이며 LLM 미구성 시 조용히 무시한다.
 */
public interface StudyCompletionReportTrigger {

	void generateForGroup(UUID groupId);
}
