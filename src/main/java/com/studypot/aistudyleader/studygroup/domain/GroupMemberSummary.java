package com.studypot.aistudyleader.studygroup.domain;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 그룹 팀원 목록 화면을 위한 멤버 요약 read-model 입니다.
 * displayName/email/onboardingStatus 는 값이 없을 수 있어 Optional 로 노출합니다.
 */
public record GroupMemberSummary(
	UUID id,
	UUID groupId,
	UUID userId,
	GroupMemberPermission permission,
	GroupMemberStatus status,
	String displayName,
	String nickname,
	String email,
	String onboardingStatus
) {

	public GroupMemberSummary {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		Objects.requireNonNull(permission, "permission must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(nickname, "nickname must not be null");
		Objects.requireNonNull(email, "email must not be null");
	}

	public Optional<String> displayNameOptional() {
		return Optional.ofNullable(displayName);
	}

	public Optional<String> onboardingStatusOptional() {
		return Optional.ofNullable(onboardingStatus);
	}
}
