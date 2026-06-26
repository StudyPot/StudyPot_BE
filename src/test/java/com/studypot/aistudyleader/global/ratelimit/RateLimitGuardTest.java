package com.studypot.aistudyleader.global.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RateLimitGuardTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000201");
	private static final RateLimitProperties.Policy USERS_ME_POLICY =
		new RateLimitProperties.Policy(60, Duration.ofSeconds(60));

	@Test
	void skipsRateLimitWhenDisabled() {
		RecordingRateLimiter limiter = new RecordingRateLimiter(RateLimitDecision.allowed(1, 60));
		RateLimitGuard guard = new RateLimitGuard(limiter, properties(false, false));

		guard.checkUsersMe(USER_ID);

		assertThat(limiter.called).isFalse();
	}

	@Test
	void checksUsersMeWithConfiguredPolicyAndKey() {
		RecordingRateLimiter limiter = new RecordingRateLimiter(RateLimitDecision.allowed(42, 60));
		RateLimitGuard guard = new RateLimitGuard(limiter, properties(true, false));

		guard.checkUsersMe(USER_ID);

		assertThat(limiter.called).isTrue();
		assertThat(limiter.key).isEqualTo("rate:users-me:user:" + USER_ID + ":60s");
		assertThat(limiter.limit).isEqualTo(60);
		assertThat(limiter.window).isEqualTo(Duration.ofSeconds(60));
	}

	@Test
	void usersMeKeyRejectsInvalidInputs() {
		assertThatThrownBy(() -> RateLimitKeyFactory.usersMe(null, Duration.ofSeconds(60)))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("userId must not be null");

		assertThatThrownBy(() -> RateLimitKeyFactory.usersMe(USER_ID, Duration.ZERO))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("window must be positive duration");
	}

	@Test
	void rejectsWhenUsersMeLimitIsExceeded() {
		RateLimitDecision rejected = RateLimitDecision.rejected(61, 60, Duration.ofSeconds(11));
		RateLimitGuard guard = new RateLimitGuard(new RecordingRateLimiter(rejected), properties(true, false));

		assertThatThrownBy(() -> guard.checkUsersMe(USER_ID))
			.isInstanceOf(RateLimitExceededException.class)
			.hasMessage("current user lookup rate limit exceeded.")
			.extracting(exception -> ((RateLimitExceededException) exception).decision())
			.isEqualTo(rejected);
	}

	@Test
	void failsOpenWhenLimiterThrowsAndFailClosedIsDisabled() {
		RateLimitGuard guard = new RateLimitGuard(new FailingRateLimiter(), properties(true, false));

		assertThatCode(() -> guard.checkUsersMe(USER_ID)).doesNotThrowAnyException();
	}

	@Test
	void failsClosedWhenLimiterThrowsAndFailClosedIsEnabled() {
		RateLimitGuard guard = new RateLimitGuard(new FailingRateLimiter(), properties(true, true));

		assertThatThrownBy(() -> guard.checkUsersMe(USER_ID))
			.isInstanceOf(RateLimitExceededException.class)
			.hasMessage("rate limit backend is unavailable.")
			.extracting(exception -> ((RateLimitExceededException) exception).decision().retryAfter())
			.isEqualTo(Duration.ofSeconds(60));
	}

	@Test
	void checkAiConversationUsesFreeDailyLimitByDefault() {
		RecordingRateLimiter limiter = new RecordingRateLimiter(RateLimitDecision.allowed(1, 15));
		RateLimitGuard guard = new RateLimitGuard(limiter, properties(true, false));

		guard.checkAiConversation(USER_ID, "FREE");

		// 마지막 호출은 일일 한도(FREE=15, 24h).
		assertThat(limiter.key).isEqualTo("rate:ai-conversation-daily:user:" + USER_ID + ":86400s");
		assertThat(limiter.limit).isEqualTo(15);
		assertThat(limiter.window).isEqualTo(Duration.ofDays(1));
	}

	@Test
	void checkAiConversationUsesPremiumDailyLimit() {
		RecordingRateLimiter limiter = new RecordingRateLimiter(RateLimitDecision.allowed(1, 200));
		RateLimitGuard guard = new RateLimitGuard(limiter, properties(true, false));

		guard.checkAiConversation(USER_ID, "premium");

		assertThat(limiter.limit).isEqualTo(200);
	}

	@Test
	void rejectsWhenDailyQuotaExceeded() {
		// 버스트(분당)는 통과시키고 일일(24h)만 거절하는 한도기로 일일 한도 초과 경로를 검증한다.
		RateLimiter limiter = (key, limit, window) -> window.toDays() >= 1
			? RateLimitDecision.rejected(limit + 1, limit, Duration.ofHours(3))
			: RateLimitDecision.allowed(1, limit);
		RateLimitGuard guard = new RateLimitGuard(limiter, properties(true, false));

		assertThatThrownBy(() -> guard.checkAiConversation(USER_ID, "FREE"))
			.isInstanceOf(RateLimitExceededException.class)
			.hasMessageContaining("AI 팀장 대화 횟수");
	}

	@Test
	void aiConversationQuotaReportsUsageWithoutIncrementing() {
		RateLimiter limiter = new RateLimiter() {
			@Override
			public RateLimitDecision check(String key, long limit, Duration window) {
				return RateLimitDecision.allowed(1, limit);
			}

			@Override
			public RateLimitSnapshot peek(String key) {
				return new RateLimitSnapshot(3, Duration.ofHours(5));
			}
		};
		RateLimitGuard guard = new RateLimitGuard(limiter, properties(true, false));

		AiConversationQuotaView view = guard.aiConversationQuota(USER_ID, "FREE");

		assertThat(view.dailyLimit()).isEqualTo(15);
		assertThat(view.used()).isEqualTo(3);
		assertThat(view.remaining()).isEqualTo(12);
		assertThat(view.resetSeconds()).isEqualTo(Duration.ofHours(5).toSeconds());
	}

	private static RateLimitProperties properties(boolean enabled, boolean failClosed) {
		return new RateLimitProperties(enabled, failClosed, USERS_ME_POLICY, null, null, null, null, null);
	}

	private static final class RecordingRateLimiter implements RateLimiter {

		private final RateLimitDecision decision;
		private boolean called;
		private String key;
		private long limit;
		private Duration window;

		private RecordingRateLimiter(RateLimitDecision decision) {
			this.decision = decision;
		}

		@Override
		public RateLimitDecision check(String key, long limit, Duration window) {
			called = true;
			this.key = key;
			this.limit = limit;
			this.window = window;
			return decision;
		}
	}

	private static final class FailingRateLimiter implements RateLimiter {

		@Override
		public RateLimitDecision check(String key, long limit, Duration window) {
			throw new IllegalStateException("Redis is down");
		}
	}
}
