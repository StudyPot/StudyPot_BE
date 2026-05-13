package com.studypot.aistudyleader.ai.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AiConversation(
	UUID id,
	UUID groupId,
	UUID memberId,
	UUID curriculumWeekId,
	UUID retrospectiveId,
	AiConversationType conversationType,
	AiConversationStatus status,
	String summary,
	Instant openedAt,
	Instant closedAt,
	Instant createdAt,
	Instant updatedAt
) {

	public AiConversation {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(conversationType, "conversationType must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(openedAt, "openedAt must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		summary = summary == null ? "" : summary.strip();
	}

	public static AiConversation open(
		UUID id,
		UUID groupId,
		UUID memberId,
		UUID curriculumWeekId,
		UUID retrospectiveId,
		AiConversationType conversationType,
		Instant now
	) {
		return new AiConversation(
			id,
			groupId,
			memberId,
			curriculumWeekId,
			retrospectiveId,
			conversationType,
			AiConversationStatus.OPEN,
			"",
			now,
			null,
			now,
			now
		);
	}
}
