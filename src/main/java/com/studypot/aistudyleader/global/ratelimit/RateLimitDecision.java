package com.studypot.aistudyleader.global.ratelimit;

import java.time.Duration;
import java.util.Objects;

public record RateLimitDecision(boolean allowed, long currentCount, long limit, Duration retryAfter) {

	public RateLimitDecision {
		if (currentCount < 0) {
			throw new IllegalArgumentException("currentCount must not be negative");
		}
		if (limit <= 0) {
			throw new IllegalArgumentException("limit must be positive");
		}
		retryAfter = Objects.requireNonNullElse(retryAfter, Duration.ZERO);
	}

	public static RateLimitDecision allowed(long currentCount, long limit) {
		return new RateLimitDecision(true, currentCount, limit, Duration.ZERO);
	}

	public static RateLimitDecision rejected(long currentCount, long limit, Duration retryAfter) {
		return new RateLimitDecision(false, currentCount, limit, retryAfter);
	}
}
