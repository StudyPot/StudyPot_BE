package com.studypot.aistudyleader.ai.domain;

import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.util.Objects;
import java.util.UUID;

public record AiConversationMembershipContext(
	UUID groupId,
	UUID memberId,
	StudyGroupStatus groupStatus,
	GroupMemberPermission permission,
	GroupMemberStatus memberStatus
) {

	public AiConversationMembershipContext {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(groupStatus, "groupStatus must not be null");
		Objects.requireNonNull(permission, "permission must not be null");
		Objects.requireNonNull(memberStatus, "memberStatus must not be null");
	}

	public boolean canOpenConversation() {
		// 온보딩을 마친(ACTIVE) 멤버는 스터디 시작 전(ONBOARDING/READY_TO_START)에도
		// AI 팀장과 대화할 수 있도록 허용한다. 종료/보관 상태에서만 막는다.
		return memberStatus == GroupMemberStatus.ACTIVE
			&& groupStatus != StudyGroupStatus.COMPLETED
			&& groupStatus != StudyGroupStatus.ARCHIVED;
	}
}
