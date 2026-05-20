package com.studypot.aistudyleader.onboarding.service;

public class OnboardingAlreadySubmittedException extends RuntimeException {

	public OnboardingAlreadySubmittedException(String message) {
		super(message);
	}
}
