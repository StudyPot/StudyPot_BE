package com.studypot.aistudyleader.curriculum.service;

import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgressStatus;
import java.util.Objects;
import java.util.UUID;

public record UpdateWeekProgressCommand(
	UUID authenticatedUserId,
	UUID weekId,
	MemberWeekProgressStatus status,
	String completionNote,
	String incompleteReason
) {

	public UpdateWeekProgressCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(weekId, "weekId must not be null");
		Objects.requireNonNull(status, "status must not be null");
	}
}
