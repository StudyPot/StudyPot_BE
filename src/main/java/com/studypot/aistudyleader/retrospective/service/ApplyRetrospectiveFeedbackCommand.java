package com.studypot.aistudyleader.retrospective.service;

import com.studypot.aistudyleader.retrospective.domain.RetrospectiveFeedbackResult;
import java.util.Objects;
import java.util.UUID;

public record ApplyRetrospectiveFeedbackCommand(
	UUID retrospectiveId,
	UUID llmUsageId,
	RetrospectiveFeedbackResult feedbackResult
) {

	public ApplyRetrospectiveFeedbackCommand {
		Objects.requireNonNull(retrospectiveId, "retrospectiveId must not be null");
		Objects.requireNonNull(llmUsageId, "llmUsageId must not be null");
		Objects.requireNonNull(feedbackResult, "feedbackResult must not be null");
	}
}
