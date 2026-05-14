package com.studypot.aistudyleader.global.ratelimit;

import java.time.Duration;

final class NoOpRateLimiter implements RateLimiter {

	@Override
	public RateLimitDecision check(String key, long limit, Duration window) {
		return RateLimitDecision.allowed(0, limit);
	}
}
