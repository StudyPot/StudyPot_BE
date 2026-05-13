package com.studypot.aistudyleader.retrospective.service;

import java.util.Objects;
import java.util.UUID;

public record FailRetrospectiveFeedbackCommand(
	UUID retrospectiveId,
	UUID llmUsageId,
	String errorCode,
	String errorMessage
) {

	public FailRetrospectiveFeedbackCommand {
		Objects.requireNonNull(retrospectiveId, "retrospectiveId must not be null");
		Objects.requireNonNull(llmUsageId, "llmUsageId must not be null");
		errorCode = requiredText("errorCode", errorCode);
		errorMessage = requiredText("errorMessage", errorMessage);
	}

	private static String requiredText(String fieldName, String value) {
		String normalized = value == null ? "" : value.strip();
		if (normalized.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank.");
		}
		return normalized;
	}
}
