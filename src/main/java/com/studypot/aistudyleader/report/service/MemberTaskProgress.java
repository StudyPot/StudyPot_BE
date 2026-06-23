package com.studypot.aistudyleader.report.service;

/**
 * 회고가 없을 때(또는 보강용) 주차 리포트 생성을 위한 한 멤버의 TODO 완료 현황입니다.
 */
public record MemberTaskProgress(String memberName, int doneCount, int totalCount) {

	public MemberTaskProgress {
		memberName = (memberName == null || memberName.isBlank()) ? "멤버" : memberName.strip();
		doneCount = Math.max(doneCount, 0);
		totalCount = Math.max(totalCount, 0);
	}
}
