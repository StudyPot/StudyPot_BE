package com.studypot.aistudyleader.studygroup.rules.domain;

import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import java.util.Objects;
import java.util.UUID;

public record GroupRuleMembership(
	UUID groupId,
	UUID memberId,
	GroupMemberPermission permission,
	GroupMemberStatus status
) {

	public GroupRuleMembership {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(permission, "permission must not be null");
		Objects.requireNonNull(status, "status must not be null");
	}

	public boolean isCurrent() {
		return status != GroupMemberStatus.LEFT;
	}
}
