package com.studypot.aistudyleader.studygroup.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 플랜별 "호스트(생성자)로서 동시에 운영 중인 스터디" 개수 한도.
 * 완료(COMPLETED)/보관(ARCHIVED)/삭제된 스터디는 한도 계산에서 제외한다.
 * 환경변수로 override: STUDYPOT_STUDYGROUP_QUOTA_FREE_MAX_HOSTED_GROUPS 등.
 */
@ConfigurationProperties(prefix = "studypot.studygroup.quota")
public record StudyGroupQuotaProperties(
	Integer freeMaxHostedGroups,
	Integer premiumMaxHostedGroups
) {

	public StudyGroupQuotaProperties {
		freeMaxHostedGroups = positiveOrDefault(freeMaxHostedGroups, 3);
		premiumMaxHostedGroups = positiveOrDefault(premiumMaxHostedGroups, 20);
	}

	/** 플랜 문자열('FREE'/'PREMIUM')에 해당하는 호스트 스터디 한도. */
	public int limitForPlan(String plan) {
		return "PREMIUM".equalsIgnoreCase(plan == null ? "" : plan.strip())
			? premiumMaxHostedGroups
			: freeMaxHostedGroups;
	}

	private static int positiveOrDefault(Integer value, int defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		if (value <= 0) {
			throw new IllegalArgumentException("study group quota must be positive: " + value);
		}
		return value;
	}
}
