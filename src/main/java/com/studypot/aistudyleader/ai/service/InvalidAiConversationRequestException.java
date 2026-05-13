package com.studypot.aistudyleader.ai.service;

public class InvalidAiConversationRequestException extends RuntimeException {

	private final String field;

	public InvalidAiConversationRequestException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String field() {
		return field;
	}
}
