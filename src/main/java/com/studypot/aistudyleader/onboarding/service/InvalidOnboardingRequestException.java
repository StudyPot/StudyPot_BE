package com.studypot.aistudyleader.onboarding.service;

public class InvalidOnboardingRequestException extends RuntimeException {

	private final String field;

	public InvalidOnboardingRequestException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String field() {
		return field;
	}
}
