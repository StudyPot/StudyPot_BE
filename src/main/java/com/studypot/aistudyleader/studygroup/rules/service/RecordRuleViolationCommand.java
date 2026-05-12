package com.studypot.aistudyleader.studygroup.rules.service;

import com.studypot.aistudyleader.studygroup.rules.domain.RuleViolationType;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record RecordRuleViolationCommand(
	UUID authenticatedUserId,
	UUID groupId,
	UUID ruleId,
	UUID memberId,
	UUID taskCompletionId,
	RuleViolationType violationType,
	Map<String, Object> details,
	Instant occurredAt
) {

	public RecordRuleViolationCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(ruleId, "ruleId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(violationType, "violationType must not be null");
		details = Map.copyOf(Objects.requireNonNull(details, "details must not be null"));
	}
}
