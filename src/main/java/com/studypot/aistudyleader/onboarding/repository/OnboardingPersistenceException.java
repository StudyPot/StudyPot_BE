package com.studypot.aistudyleader.onboarding.repository;

public class OnboardingPersistenceException extends RuntimeException {

	public OnboardingPersistenceException(String message) {
		super(message);
	}

	public OnboardingPersistenceException(String message, Throwable cause) {
		super(message, cause);
	}
}
