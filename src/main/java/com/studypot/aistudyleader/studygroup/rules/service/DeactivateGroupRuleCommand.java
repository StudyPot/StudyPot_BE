package com.studypot.aistudyleader.studygroup.rules.service;

import java.util.Objects;
import java.util.UUID;

public record DeactivateGroupRuleCommand(UUID authenticatedUserId, UUID groupId, UUID ruleId) {

	public DeactivateGroupRuleCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(ruleId, "ruleId must not be null");
	}
}
