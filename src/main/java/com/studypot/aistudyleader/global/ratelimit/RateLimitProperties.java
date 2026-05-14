package com.studypot.aistudyleader.global.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "studypot.rate-limit")
public record RateLimitProperties(
	Boolean enabled,
	Boolean failClosed,
	Policy usersMe,
	Policy aiConversation,
	Policy curriculumGeneration,
	Policy retrospectiveFeedback
) {

	public RateLimitProperties {
		enabled = Boolean.TRUE.equals(enabled);
		failClosed = failClosed == null || failClosed;
		usersMe = policyOrDefault(usersMe, 60, Duration.ofMinutes(1));
		aiConversation = policyOrDefault(aiConversation, 5, Duration.ofMinutes(1));
		curriculumGeneration = policyOrDefault(curriculumGeneration, 3, Duration.ofMinutes(10));
		retrospectiveFeedback = policyOrDefault(retrospectiveFeedback, 2, Duration.ofDays(1));
	}

	private static Policy policyOrDefault(Policy policy, long limit, Duration window) {
		return policy == null ? new Policy(limit, window) : policy;
	}

	public record Policy(long limit, Duration window) {

		public Policy {
			if (limit <= 0) {
				throw new IllegalArgumentException("rate limit must be positive");
			}
			if (window == null || window.isZero() || window.isNegative()) {
				throw new IllegalArgumentException("rate limit window must be positive");
			}
		}
	}
}
