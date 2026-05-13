package com.studypot.aistudyleader.retrospective.domain;

import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.util.Objects;
import java.util.UUID;

public record RetrospectiveMembershipContext(
	UUID groupId,
	UUID memberId,
	StudyGroupStatus groupStatus,
	GroupMemberPermission permission,
	GroupMemberStatus memberStatus
) {

	public RetrospectiveMembershipContext {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(groupStatus, "groupStatus must not be null");
		Objects.requireNonNull(permission, "permission must not be null");
		Objects.requireNonNull(memberStatus, "memberStatus must not be null");
	}

	public boolean hasActiveMembership() {
		return memberStatus == GroupMemberStatus.ACTIVE;
	}

	public boolean canRequestRetrospective() {
		return groupStatus == StudyGroupStatus.ACTIVE && hasActiveMembership();
	}
}
