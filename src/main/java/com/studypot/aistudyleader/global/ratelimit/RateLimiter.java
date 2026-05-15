package com.studypot.aistudyleader.global.ratelimit;

import java.time.Duration;

public interface RateLimiter {

	RateLimitDecision check(String key, long limit, Duration window);
}
