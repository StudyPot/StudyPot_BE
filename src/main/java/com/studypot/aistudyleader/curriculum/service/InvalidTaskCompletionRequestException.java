package com.studypot.aistudyleader.curriculum.service;

public class InvalidTaskCompletionRequestException extends RuntimeException {

	private final String field;

	public InvalidTaskCompletionRequestException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String field() {
		return field;
	}
}
