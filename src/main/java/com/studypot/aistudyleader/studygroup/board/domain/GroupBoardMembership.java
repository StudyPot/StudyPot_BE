package com.studypot.aistudyleader.studygroup.board.domain;

import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import java.util.Objects;
import java.util.UUID;

public record GroupBoardMembership(
	UUID groupId,
	UUID memberId,
	UUID userId,
	String displayName,
	GroupMemberPermission permission,
	GroupMemberStatus status
) {

	public GroupBoardMembership(
		UUID groupId,
		UUID memberId,
		GroupMemberPermission permission,
		GroupMemberStatus status
	) {
		this(groupId, memberId, null, null, permission, status);
	}

	public GroupBoardMembership {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(permission, "permission must not be null");
		Objects.requireNonNull(status, "status must not be null");
		displayName = displayName == null || displayName.isBlank() ? null : displayName.strip();
	}

	public boolean active() {
		return status == GroupMemberStatus.ACTIVE;
	}

	public boolean owner() {
		return permission == GroupMemberPermission.OWNER;
	}
}
