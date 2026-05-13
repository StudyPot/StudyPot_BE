package com.studypot.aistudyleader.ai.service;

import com.studypot.aistudyleader.ai.domain.AiConversationType;
import java.util.Objects;
import java.util.UUID;

public record OpenAiConversationCommand(
	UUID authenticatedUserId,
	UUID groupId,
	AiConversationType conversationType,
	UUID weekId,
	UUID retrospectiveId
) {

	public OpenAiConversationCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		if (conversationType == null) {
			throw new InvalidAiConversationRequestException("conversationType", "conversationType is required.");
		}
	}
}
