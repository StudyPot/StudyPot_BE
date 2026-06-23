package com.studypot.aistudyleader.report.service;

import java.util.Objects;

/**
 * 주차 리포트 생성을 위한 한 멤버의 회고 요약 입력입니다.
 */
public record MemberRetrospectiveSummary(String memberName, String summary) {

	public MemberRetrospectiveSummary {
		memberName = (memberName == null || memberName.isBlank()) ? "멤버" : memberName.strip();
		summary = Objects.requireNonNullElse(summary, "").strip();
	}
}
