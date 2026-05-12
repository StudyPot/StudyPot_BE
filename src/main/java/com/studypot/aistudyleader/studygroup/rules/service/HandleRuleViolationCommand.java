package com.studypot.aistudyleader.studygroup.rules.service;

import java.util.Objects;
import java.util.UUID;

public record HandleRuleViolationCommand(
	UUID authenticatedUserId,
	UUID groupId,
	UUID violationId,
	String note
) {

	public HandleRuleViolationCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(violationId, "violationId must not be null");
	}
}
