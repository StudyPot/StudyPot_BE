package com.studypot.aistudyleader.curriculum.infrastructure.openai;

public class OpenAiClientException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public OpenAiClientException(String message) {
		super(message);
	}

	public OpenAiClientException(String message, Throwable cause) {
		super(message, cause);
	}
}
