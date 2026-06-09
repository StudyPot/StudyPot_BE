package com.studypot.aistudyleader.studygroup.service;

import java.util.Objects;
import java.util.UUID;

public record DeleteStudyGroupCommand(UUID authenticatedUserId, UUID groupId) {

	public DeleteStudyGroupCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
	}
}
