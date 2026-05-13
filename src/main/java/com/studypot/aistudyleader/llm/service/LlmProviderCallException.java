package com.studypot.aistudyleader.llm.service;

import java.util.Objects;

public class LlmProviderCallException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final transient LlmCallFailure failure;

	public LlmProviderCallException(String message, LlmCallFailure failure) {
		super(message);
		this.failure = Objects.requireNonNull(failure, "failure must not be null");
	}

	public LlmProviderCallException(String message, Throwable cause, LlmCallFailure failure) {
		super(message, cause);
		this.failure = Objects.requireNonNull(failure, "failure must not be null");
	}

	public LlmCallFailure failure() {
		return failure;
	}
}
