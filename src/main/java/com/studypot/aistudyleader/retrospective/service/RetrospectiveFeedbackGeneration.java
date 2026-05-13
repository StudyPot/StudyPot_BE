package com.studypot.aistudyleader.retrospective.service;

import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveFeedbackResult;
import java.util.Objects;

record RetrospectiveFeedbackGeneration(
	RetrospectiveFeedbackResult feedbackResult,
	LlmStructuredResponse response
) {

	RetrospectiveFeedbackGeneration {
		Objects.requireNonNull(feedbackResult, "feedbackResult must not be null");
		Objects.requireNonNull(response, "response must not be null");
	}
}
