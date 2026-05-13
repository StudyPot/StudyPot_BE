package com.studypot.aistudyleader.curriculum.service;

import com.studypot.aistudyleader.llm.service.LlmCallFailure;
import java.util.Optional;

public class CurriculumGenerationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final LlmCallFailure failure;

	public CurriculumGenerationException(String message) {
		super(message);
		this.failure = null;
	}

	public CurriculumGenerationException(String message, Throwable cause) {
		super(message, cause);
		this.failure = null;
	}

	public CurriculumGenerationException(String message, LlmCallFailure failure) {
		super(message);
		this.failure = failure;
	}

	public CurriculumGenerationException(String message, Throwable cause, LlmCallFailure failure) {
		super(message, cause);
		this.failure = failure;
	}

	public Optional<LlmCallFailure> failure() {
		return Optional.ofNullable(failure);
	}
}
