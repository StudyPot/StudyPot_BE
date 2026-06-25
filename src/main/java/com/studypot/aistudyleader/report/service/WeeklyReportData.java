package com.studypot.aistudyleader.report.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 한 주차의 멤버 회고 + TODO 완료 현황을 바탕으로 주차 리포트를 생성하기 위한 입력입니다.
 * 완료된 회고가 없어도 memberTaskProgress(각 멤버의 TODO 완료 현황)로 리포트를 만들 수 있습니다.
 */
public record WeeklyReportData(
	UUID groupId,
	UUID weekId,
	int weekNumber,
	String weekTitle,
	List<MemberRetrospectiveSummary> memberRetrospectives,
	List<MemberTaskProgress> memberTaskProgress
) {

	public WeeklyReportData {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(weekId, "weekId must not be null");
		weekTitle = (weekTitle == null || weekTitle.isBlank()) ? (weekNumber + "주차") : weekTitle.strip();
		memberRetrospectives = List.copyOf(Objects.requireNonNull(memberRetrospectives, "memberRetrospectives must not be null"));
		memberTaskProgress = List.copyOf(Objects.requireNonNull(memberTaskProgress, "memberTaskProgress must not be null"));
	}
}
