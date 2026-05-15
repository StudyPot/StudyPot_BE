package com.studypot.aistudyleader.global.ratelimit;

import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RateLimitGuard {

	private static final Logger log = LoggerFactory.getLogger(RateLimitGuard.class);
	private static final String USERS_ME_LIMIT_MESSAGE = "current user lookup rate limit exceeded.";

	private final RateLimiter rateLimiter;
	private final RateLimitProperties properties;

	public RateLimitGuard(RateLimiter rateLimiter, RateLimitProperties properties) {
		this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter must not be null");
		this.properties = Objects.requireNonNull(properties, "properties must not be null");
	}

	public void checkUsersMe(UUID userId) {
		Objects.requireNonNull(userId, "userId must not be null");
		if (!properties.enabled()) {
			return;
		}
		RateLimitProperties.Policy policy = properties.usersMe();
		String key = RateLimitKeyFactory.usersMe(userId, policy.window());
		RateLimitDecision decision = check(key, policy);
		if (!decision.allowed()) {
			log.warn(
				"Rate limit rejected: key={}, limit={}, window={}, currentCount={}, retryAfter={}",
				key,
				policy.limit(),
				policy.window(),
				decision.currentCount(),
				decision.retryAfter()
			);
			throw new RateLimitExceededException(USERS_ME_LIMIT_MESSAGE, decision);
		}
	}

	private RateLimitDecision check(String key, RateLimitProperties.Policy policy) {
		try {
			return rateLimiter.check(key, policy.limit(), policy.window());
		} catch (RuntimeException exception) {
			log.warn(
				"Rate limit backend error: key={}, limit={}, window={}, failClosed={}",
				key,
				policy.limit(),
				policy.window(),
				properties.failClosed(),
				exception
			);
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
