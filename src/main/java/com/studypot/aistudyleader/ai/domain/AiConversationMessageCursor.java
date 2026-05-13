package com.studypot.aistudyleader.ai.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AiConversationMessageCursor(Instant createdAt, UUID id) {

	public AiConversationMessageCursor {
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(id, "id must not be null");
	}
}
