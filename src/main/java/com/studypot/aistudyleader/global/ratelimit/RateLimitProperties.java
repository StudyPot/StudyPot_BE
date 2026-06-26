package com.studypot.aistudyleader.global.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "studypot.rate-limit")
public record RateLimitProperties(
	Boolean enabled,
	Boolean failClosed,
	Policy usersMe,
	Policy aiConversation,
	Policy aiConversationDailyFree,
	Policy aiConversationDailyPremium,
	Policy curriculumGeneration,
	Policy retrospectiveFeedback
) {

	public RateLimitProperties {
		enabled = Boolean.TRUE.equals(enabled);
		failClosed = failClosed == null || failClosed;
		usersMe = policyOrDefault(usersMe, 60, Duration.ofMinutes(1));
		// 버스트(분당) 한도 — 플랜 무관 스팸 방지.
		aiConversation = policyOrDefault(aiConversation, 10, Duration.ofMinutes(1));
		// 일일(롤링 24h) 한도 — 플랜별. 비용의 대부분을 차지하는 AI 팀장 채팅을 묶는다.
		aiConversationDailyFree = policyOrDefault(aiConversationDailyFree, 15, Duration.ofDays(1));
		aiConversationDailyPremium = policyOrDefault(aiConversationDailyPremium, 200, Duration.ofDays(1));
		curriculumGeneration = policyOrDefault(curriculumGeneration, 3, Duration.ofMinutes(10));
		retrospectiveFeedback = policyOrDefault(retrospectiveFeedback, 2, Duration.ofDays(1));
	}

	/** 플랜('FREE'/'PREMIUM')에 해당하는 일일 채팅 한도. */
	public Policy aiConversationDailyFor(String plan) {
		return "PREMIUM".equalsIgnoreCase(plan == null ? "" : plan.strip())
			? aiConversationDailyPremium
			: aiConversationDailyFree;
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
