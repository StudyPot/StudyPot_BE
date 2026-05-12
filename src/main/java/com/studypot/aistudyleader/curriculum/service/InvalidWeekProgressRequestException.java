package com.studypot.aistudyleader.curriculum.service;

public class InvalidWeekProgressRequestException extends RuntimeException {

	private final String field;

	public InvalidWeekProgressRequestException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String field() {
		return field;
	}
}
