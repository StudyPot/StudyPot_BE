package com.studypot.aistudyleader.retrospective.service;

import com.studypot.aistudyleader.retrospective.domain.RetrospectiveTriggerType;
import java.util.Objects;
import java.util.UUID;

public record RequestRetrospectiveCommand(
	UUID authenticatedUserId,
	UUID weekId,
	RetrospectiveTriggerType triggerType
) {

	public RequestRetrospectiveCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(weekId, "weekId must not be null");
		triggerType = triggerType == null ? RetrospectiveTriggerType.MANUAL : triggerType;
	}
}
