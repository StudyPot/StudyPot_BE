package com.studypot.aistudyleader.global.ratelimit;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;

public record RateLimitDecision(boolean allowed, long currentCount, long limit, Duration retryAfter) implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	public RateLimitDecision {
		if (currentCount < 0) {
			throw new IllegalArgumentException("currentCount must not be negative");
		}
		if (limit <= 0) {
			throw new IllegalArgumentException("limit must be positive");
		}
		retryAfter = Objects.requireNonNull(retryAfter, "retryAfter must not be null");
		if (retryAfter.isNegative()) {
			throw new IllegalArgumentException("retryAfter must not be negative");
		}
		if (allowed && !retryAfter.isZero()) {
			throw new IllegalArgumentException("allowed decision must not have retryAfter");
		}
		if (!allowed && retryAfter.isZero()) {
			throw new IllegalArgumentException("rejected decision must have positive retryAfter");
		}
		if (!allowed && currentCount <= limit) {
			throw new IllegalArgumentException("rejected decision must exceed limit");
		}
	}

	public static RateLimitDecision allowed(long currentCount, long limit) {
		return new RateLimitDecision(true, currentCount, limit, Duration.ZERO);
	}

	public static RateLimitDecision rejected(long currentCount, long limit, Duration retryAfter) {
		Objects.requireNonNull(retryAfter, "retryAfter must not be null");
		return new RateLimitDecision(false, currentCount, limit, retryAfter);
	}
}
