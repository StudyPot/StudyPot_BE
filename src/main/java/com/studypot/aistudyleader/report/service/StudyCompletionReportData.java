package com.studypot.aistudyleader.report.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 스터디 완료(수료) 리포트 생성 입력: 전체 스터디(모든 주차)의 멤버 회고 + TODO 완료 현황.
 */
public record StudyCompletionReportData(
	UUID groupId,
	String studyName,
	int totalWeeks,
	List<MemberRetrospectiveSummary> memberRetrospectives,
	List<MemberTaskProgress> memberTaskProgress
) {

	public StudyCompletionReportData {
		Objects.requireNonNull(groupId, "groupId must not be null");
		studyName = (studyName == null || studyName.isBlank()) ? "스터디" : studyName.strip();
		totalWeeks = Math.max(totalWeeks, 0);
		memberRetrospectives = List.copyOf(Objects.requireNonNull(memberRetrospectives, "memberRetrospectives must not be null"));
		memberTaskProgress = List.copyOf(Objects.requireNonNull(memberTaskProgress, "memberTaskProgress must not be null"));
	}
}
