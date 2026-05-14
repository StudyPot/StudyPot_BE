package com.studypot.aistudyleader.global.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisRateLimiterTest {

	private static final String KEY = "rate:users-me:user:018f0000-0000-7000-8000-000000000201:60s";
	private static final Duration WINDOW = Duration.ofSeconds(60);

	private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
	private final ValueOperations<String, String> valueOperations = mockValueOperations();
	private final RedisRateLimiter limiter = new RedisRateLimiter(redisTemplate);

	@Test
	void firstRequestStartsWindowAndIsAllowed() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.increment(KEY)).thenReturn(1L);

		RateLimitDecision decision = limiter.check(KEY, 60, WINDOW);

		assertThat(decision.allowed()).isTrue();
		assertThat(decision.currentCount()).isEqualTo(1);
		assertThat(decision.limit()).isEqualTo(60);
		assertThat(decision.retryAfter()).isZero();
		verify(redisTemplate).expire(KEY, 60, TimeUnit.SECONDS);
	}

	@Test
	void rejectsRequestOverLimitWithRemainingTtl() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.increment(KEY)).thenReturn(61L);
		when(redisTemplate.getExpire(KEY)).thenReturn(12L);

		RateLimitDecision decision = limiter.check(KEY, 60, WINDOW);

		assertThat(decision.allowed()).isFalse();
		assertThat(decision.currentCount()).isEqualTo(61);
		assertThat(decision.limit()).isEqualTo(60);
		assertThat(decision.retryAfter()).isEqualTo(Duration.ofSeconds(12));
		verify(redisTemplate, never()).expire(KEY, 60, TimeUnit.SECONDS);
	}

	@Test
	void restoresExpiryWhenOverLimitCounterHasNoTtl() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.increment(KEY)).thenReturn(61L);
		when(redisTemplate.getExpire(KEY)).thenReturn(-1L);

		RateLimitDecision decision = limiter.check(KEY, 60, WINDOW);

		assertThat(decision.allowed()).isFalse();
		assertThat(decision.retryAfter()).isEqualTo(WINDOW);
		verify(redisTemplate).expire(KEY, 60, TimeUnit.SECONDS);
	}

	@SuppressWarnings("unchecked")
	private static ValueOperations<String, String> mockValueOperations() {
		return mock(ValueOperations.class);
	}
}
