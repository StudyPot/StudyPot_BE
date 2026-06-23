package com.studypot.aistudyleader.report.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 한 주차의 모든 멤버 회고를 바탕으로 주차 리포트를 생성하기 위한 입력입니다.
 */
public record WeeklyReportData(
	UUID groupId,
	UUID weekId,
	int weekNumber,
	String weekTitle,
	List<MemberRetrospectiveSummary> memberRetrospectives
) {

	public WeeklyReportData {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(weekId, "weekId must not be null");
		weekTitle = (weekTitle == null || weekTitle.isBlank()) ? (weekNumber + "주차") : weekTitle.strip();
		memberRetrospectives = List.copyOf(Objects.requireNonNull(memberRetrospectives, "memberRetrospectives must not be null"));
	}
}
