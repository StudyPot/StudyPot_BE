package com.studypot.aistudyleader.curriculum.service;

import com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus;
import java.util.Objects;
import java.util.UUID;

public record CompleteTaskCommand(
	UUID authenticatedUserId,
	UUID taskId,
	TaskCompletionStatus status,
	String completionNote,
	String incompleteReason,
	String evidenceUrl
) {

	public CompleteTaskCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(taskId, "taskId must not be null");
	}
}
