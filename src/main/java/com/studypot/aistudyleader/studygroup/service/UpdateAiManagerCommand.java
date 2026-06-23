package com.studypot.aistudyleader.studygroup.service;

import java.util.Objects;
import java.util.UUID;

public record UpdateAiManagerCommand(
	UUID authenticatedUserId,
	UUID groupId,
	String persona
) {

	public UpdateAiManagerCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(persona, "persona must not be null");
	}
}
