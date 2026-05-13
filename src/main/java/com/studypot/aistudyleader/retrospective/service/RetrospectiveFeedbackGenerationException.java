package com.studypot.aistudyleader.retrospective.service;

import com.studypot.aistudyleader.llm.service.LlmCallFailure;
import java.util.Objects;

class RetrospectiveFeedbackGenerationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final transient LlmCallFailure failure;

	RetrospectiveFeedbackGenerationException(String message, LlmCallFailure failure) {
		super(message);
		this.failure = Objects.requireNonNull(failure, "failure must not be null");
	}

	RetrospectiveFeedbackGenerationException(String message, Throwable cause, LlmCallFailure failure) {
		super(message, cause);
		this.failure = Objects.requireNonNull(failure, "failure must not be null");
	}

	LlmCallFailure failure() {
		return failure;
	}
}
