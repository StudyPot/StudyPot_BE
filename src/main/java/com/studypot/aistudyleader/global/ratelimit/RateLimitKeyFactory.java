package com.studypot.aistudyleader.global.ratelimit;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

final class RateLimitKeyFactory {

	private RateLimitKeyFactory() {
	}

	static String usersMe(UUID userId, Duration window) {
		Objects.requireNonNull(userId, "userId must not be null");
		Objects.requireNonNull(window, "window must not be null");
		if (window.isZero() || window.isNegative()) {
			throw new IllegalArgumentException("window must be positive duration");
		}
		return "rate:users-me:user:" + userId + ":" + window.toSeconds() + "s";
	}
}
