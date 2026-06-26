package com.studypot.aistudyleader.global.ratelimit;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

final class RateLimitKeyFactory {

	private RateLimitKeyFactory() {
	}

	static String usersMe(UUID userId, Duration window) {
		return key("users-me", userId, window);
	}

	/** AI 팀장 채팅 버스트(분당) 한도 키. */
	static String aiConversationBurst(UUID userId, Duration window) {
		return key("ai-conversation", userId, window);
	}

	/** AI 팀장 채팅 일일(롤링 24h) 한도 키. 플랜과 무관하게 사용자별로 1개의 윈도우를 공유한다. */
	static String aiConversationDaily(UUID userId, Duration window) {
		return key("ai-conversation-daily", userId, window);
	}

	private static String key(String scope, UUID userId, Duration window) {
		Objects.requireNonNull(userId, "userId must not be null");
		Objects.requireNonNull(window, "window must not be null");
		if (window.isZero() || window.isNegative()) {
			throw new IllegalArgumentException("window must be positive duration");
		}
		return "rate:" + scope + ":user:" + userId + ":" + window.toSeconds() + "s";
	}
}
