package com.studypot.aistudyleader.onboarding.domain;

import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 팀원 탭에서 멤버별 온보딩 상태를 닉네임, 역할, 가입일과 함께 노출하기 위한 조회 결과입니다.
 * 가입했지만 아직 온보딩 응답이 없는 멤버도 포함되며, 그 경우 response 는 null 입니다.
 */
public record GroupMemberOnboarding(
	UUID memberId,
	String memberNickname,
	GroupMemberStatus memberStatus,
	GroupMemberPermission permission,
	Instant joinedAt,
	GroupOnboardingResponse response
) {

	public GroupMemberOnboarding {
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(memberStatus, "memberStatus must not be null");
	}

	public Optional<GroupOnboardingResponse> responseOptional() {
		return Optional.ofNullable(response);
	}
}
