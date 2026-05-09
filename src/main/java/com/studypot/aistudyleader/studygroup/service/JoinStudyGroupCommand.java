package com.studypot.aistudyleader.studygroup.service;

import java.util.Objects;
import java.util.UUID;

public record JoinStudyGroupCommand(
	UUID authenticatedUserId,
	UUID groupId,
	String inviteCode
) {

	public JoinStudyGroupCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		if (inviteCode == null || inviteCode.isBlank()) {
			throw new IllegalArgumentException("inviteCode must not be blank");
		}
		inviteCode = inviteCode.strip();
	}
}
