package com.studypot.aistudyleader.studygroup.service;

import java.util.Objects;
import java.util.UUID;

public record JoinStudyGroupByInviteCodeCommand(
	UUID authenticatedUserId,
	String inviteCode
) {

	public JoinStudyGroupByInviteCodeCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		if (inviteCode == null || inviteCode.isBlank()) {
			throw new IllegalArgumentException("inviteCode must not be blank");
		}
		inviteCode = inviteCode.strip();
	}
}
