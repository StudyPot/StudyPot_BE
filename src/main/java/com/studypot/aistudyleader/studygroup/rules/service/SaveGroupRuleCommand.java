package com.studypot.aistudyleader.studygroup.rules.service;

import com.studypot.aistudyleader.studygroup.rules.domain.GroupRuleType;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record SaveGroupRuleCommand(
	UUID authenticatedUserId,
	UUID groupId,
	GroupRuleType ruleType,
	Map<String, Object> config,
	String description,
	boolean active
) {

	public SaveGroupRuleCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(ruleType, "ruleType must not be null");
		config = Map.copyOf(Objects.requireNonNull(config, "config must not be null"));
		if (config.isEmpty()) {
			throw new InvalidGroupRuleRequestException("config", "config must not be empty.");
		}
	}
}
