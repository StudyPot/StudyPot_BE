package com.studypot.aistudyleader.global.ratelimit;

import java.util.Objects;

public class RateLimitExceededException extends RuntimeException {

	private final RateLimitDecision decision;

	public RateLimitExceededException(String message, RateLimitDecision decision) {
		super(message);
		this.decision = Objects.requireNonNull(decision, "decision must not be null");
	}

	public RateLimitExceededException(String message, RateLimitDecision decision, Throwable cause) {
		super(message, cause);
		this.decision = Objects.requireNonNull(decision, "decision must not be null");
	}

	public RateLimitDecision decision() {
		return decision;
	}
}
