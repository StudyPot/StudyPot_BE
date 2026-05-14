package com.studypot.aistudyleader.global.ratelimit;

import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RateLimitGuard {

	private static final String USERS_ME_LIMIT_MESSAGE = "current user lookup rate limit exceeded.";

	private final RateLimiter rateLimiter;
	private final RateLimitProperties properties;

	public RateLimitGuard(RateLimiter rateLimiter, RateLimitProperties properties) {
		this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter must not be null");
		this.properties = Objects.requireNonNull(properties, "properties must not be null");
	}

	public void checkUsersMe(UUID userId) {
		if (!properties.enabled()) {
			return;
		}
		RateLimitProperties.Policy policy = properties.usersMe();
		RateLimitDecision decision = check(RateLimitKeyFactory.usersMe(userId, policy.window()), policy);
		if (!decision.allowed()) {
			throw new RateLimitExceededException(USERS_ME_LIMIT_MESSAGE, decision);
		}
	}

	private RateLimitDecision check(String key, RateLimitProperties.Policy policy) {
		try {
			return rateLimiter.check(key, policy.limit(), policy.window());
		} catch (RuntimeException exception) {
			if (!properties.failClosed()) {
				return RateLimitDecision.allowed(0, policy.limit());
			}
			throw new RateLimitExceededException(
				"rate limit backend is unavailable.",
				RateLimitDecision.rejected(policy.limit() + 1, policy.limit(), policy.window()),
				exception
			);
		}
	}
}
