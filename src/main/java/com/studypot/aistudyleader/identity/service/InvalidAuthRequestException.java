package com.studypot.aistudyleader.identity.service;

public class InvalidAuthRequestException extends RuntimeException {

	private final String field;

	public InvalidAuthRequestException(String message) {
		this("request", message);
	}

	public InvalidAuthRequestException(String field, String message) {
		super(message);
		this.field = field == null || field.isBlank() ? "request" : field.strip();
	}

	public String field() {
		return field;
	}
}
