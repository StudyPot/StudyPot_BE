package com.studypot.aistudyleader.global.ratelimit;

import java.io.Serial;
import java.util.Objects;

public class RateLimitExceededException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

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
