package com.studypot.aistudyleader.global.ratelimit;

import java.time.Duration;
import java.util.UUID;

final class RateLimitKeyFactory {

	private RateLimitKeyFactory() {
	}

	static String usersMe(UUID userId, Duration window) {
		return "rate:users-me:user:" + userId + ":" + window.toSeconds() + "s";
	}
}
