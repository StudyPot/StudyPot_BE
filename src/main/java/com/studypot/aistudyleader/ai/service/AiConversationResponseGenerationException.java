package com.studypot.aistudyleader.ai.service;

import com.studypot.aistudyleader.llm.service.LlmCallFailure;
import java.util.Objects;

public class AiConversationResponseGenerationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final transient LlmCallFailure failure;

	public AiConversationResponseGenerationException(String message, LlmCallFailure failure) {
		super(message);
		this.failure = Objects.requireNonNull(failure, "failure must not be null");
	}

	public AiConversationResponseGenerationException(String message, Throwable cause, LlmCallFailure failure) {
		super(message, cause);
		this.failure = Objects.requireNonNull(failure, "failure must not be null");
	}

	public LlmCallFailure failure() {
		return failure;
	}
}
