package com.studypot.aistudyleader.curriculum.service;

import java.util.Objects;
import java.util.UUID;

public record StartCurriculumCommand(
	UUID authenticatedUserId,
	UUID groupId
) {

	public StartCurriculumCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
	}
}
