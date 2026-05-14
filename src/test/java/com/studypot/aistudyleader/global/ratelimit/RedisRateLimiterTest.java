package com.studypot.aistudyleader.global.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

class RedisRateLimiterTest {

	private static final String KEY = "rate:users-me:user:018f0000-0000-7000-8000-000000000201:60s";
	private static final Duration WINDOW = Duration.ofSeconds(60);

	private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
	private final RedisRateLimiter limiter = new RedisRateLimiter(redisTemplate);

	@Test
	void firstRequestStartsWindowAndIsAllowed() {
		when(redisTemplate.execute(any(RedisScript.class), eq(List.of(KEY)), eq("60000"))).thenReturn(1L);

		RateLimitDecision decision = limiter.check(KEY, 60, WINDOW);

		assertThat(decision.allowed()).isTrue();
		assertThat(decision.currentCount()).isEqualTo(1);
		assertThat(decision.limit()).isEqualTo(60);
		assertThat(decision.retryAfter()).isZero();
		verify(redisTemplate, never()).getExpire(KEY, TimeUnit.MILLISECONDS);
	}

	@Test
	void rejectsRequestOverLimitWithRemainingTtl() {
		when(redisTemplate.execute(any(RedisScript.class), eq(List.of(KEY)), eq("60000"))).thenReturn(61L);
		when(redisTemplate.getExpire(KEY, TimeUnit.MILLISECONDS)).thenReturn(12_000L);

		RateLimitDecision decision = limiter.check(KEY, 60, WINDOW);

		assertThat(decision.allowed()).isFalse();
		assertThat(decision.currentCount()).isEqualTo(61);
		assertThat(decision.limit()).isEqualTo(60);
		assertThat(decision.retryAfter()).isEqualTo(Duration.ofSeconds(12));
		verify(redisTemplate, never()).delete(KEY);
	}

	@Test
	void restoresExpiryWhenOverLimitCounterHasNoTtl() {
		when(redisTemplate.execute(any(RedisScript.class), eq(List.of(KEY)), eq("60000"))).thenReturn(61L);
		when(redisTemplate.getExpire(KEY, TimeUnit.MILLISECONDS)).thenReturn(-1L);

		RateLimitDecision decision = limiter.check(KEY, 60, WINDOW);

		assertThat(decision.allowed()).isFalse();
		assertThat(decision.retryAfter()).isEqualTo(WINDOW);
		verify(redisTemplate).delete(KEY);
	}

	@Test
	void rejectsInvalidPolicyInputs() {
		assertThatThrownBy(() -> limiter.check(KEY, 0, WINDOW))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("limit must be positive");

		assertThatThrownBy(() -> limiter.check(KEY, 60, Duration.ZERO))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("window must be positive");
	}
}
