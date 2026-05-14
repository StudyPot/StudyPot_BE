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

	private static RateLimitProperties properties(boolean enabled, boolean failClosed) {
		return new RateLimitProperties(enabled, failClosed, USERS_ME_POLICY, null, null, null);
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
