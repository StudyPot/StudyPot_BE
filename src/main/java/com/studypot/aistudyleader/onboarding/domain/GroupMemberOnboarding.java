package com.studypot.aistudyleader.onboarding.domain;

import java.util.Objects;

/**
 * 팀원 탭에서 멤버별 온보딩 응답을 닉네임과 함께 노출하기 위한 조회 결과입니다.
 */
public record GroupMemberOnboarding(
	String memberNickname,
	GroupOnboardingResponse response
) {

	public GroupMemberOnboarding {
		Objects.requireNonNull(response, "response must not be null");
	}
}
