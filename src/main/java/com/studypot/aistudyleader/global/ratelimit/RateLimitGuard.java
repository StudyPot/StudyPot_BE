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
	private static final String AI_CONVERSATION_BURST_MESSAGE = "AI 팀장과 너무 빠르게 대화하고 있어요. 잠시 후 다시 시도해 주세요.";
	private static final String AI_CONVERSATION_DAILY_MESSAGE = "오늘 사용할 수 있는 AI 팀장 대화 횟수를 모두 사용했어요. 내일 다시 이용하거나 플랜을 업그레이드해 주세요.";

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

	/**
	 * AI 팀장 채팅 한도 검사: 버스트(분당, 플랜 무관) → 일일(롤링 24h, 플랜별) 순.
	 * 초과 시 {@link RateLimitExceededException}(HTTP 429)을 던진다.
	 */
	public void checkAiConversation(UUID userId, String plan) {
		Objects.requireNonNull(userId, "userId must not be null");
		if (!properties.enabled()) {
			return;
		}
		RateLimitProperties.Policy burst = properties.aiConversation();
		enforce(RateLimitKeyFactory.aiConversationBurst(userId, burst.window()), burst, AI_CONVERSATION_BURST_MESSAGE);

		RateLimitProperties.Policy daily = properties.aiConversationDailyFor(plan);
		enforce(RateLimitKeyFactory.aiConversationDaily(userId, daily.window()), daily, AI_CONVERSATION_DAILY_MESSAGE);
	}

	/**
	 * AI 팀장 채팅 일일 한도의 현재 사용 현황을 (증가 없이) 조회한다. FE 잔여 횟수 표시용.
	 * rate limit 이 꺼져 있거나 백엔드 오류 시에는 사용량 0(=잔여 = 한도)으로 본다.
	 */
	public AiConversationQuotaView aiConversationQuota(UUID userId, String plan) {
		Objects.requireNonNull(userId, "userId must not be null");
		RateLimitProperties.Policy daily = properties.aiConversationDailyFor(plan);
		long limit = daily.limit();
		if (!properties.enabled()) {
			return new AiConversationQuotaView(limit, 0, limit, 0);
		}
		String key = RateLimitKeyFactory.aiConversationDaily(userId, daily.window());
		RateLimitSnapshot snapshot;
		try {
			snapshot = rateLimiter.peek(key);
		} catch (RuntimeException exception) {
			log.warn("Rate limit peek failed: key={}", key, exception);
			snapshot = new RateLimitSnapshot(0, java.time.Duration.ZERO);
		}
		long used = Math.min(snapshot.count(), limit);
		long remaining = Math.max(0, limit - snapshot.count());
		return new AiConversationQuotaView(limit, used, remaining, snapshot.timeToReset().toSeconds());
	}

	private void enforce(String key, RateLimitProperties.Policy policy, String message) {
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
			throw new RateLimitExceededException(message, decision);
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
