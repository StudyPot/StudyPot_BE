package com.studypot.aistudyleader.review.domain;

import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import java.util.Objects;
import java.util.UUID;

public record ReviewMembership(
	UUID groupId,
	UUID memberId,
	UUID userId,
	String displayName,
	GroupMemberStatus status
) {

	public ReviewMembership {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		Objects.requireNonNull(status, "status must not be null");
		displayName = displayName == null || displayName.isBlank() ? null : displayName.strip();
	}

	public boolean active() {
		return status == GroupMemberStatus.ACTIVE;
	}
}
