package com.studypot.aistudyleader.report.service;

/**
 * 스터디 완료 시 전체 스터디를 종합한 '수료 리포트'를 생성한다.
 */
public interface StudyCompletionReportGenerator {

	WeeklyReportGeneration generate(StudyCompletionReportData data);
}
