package com.studypot.aistudyleader.global.ratelimit;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;

final class RedisRateLimiter implements RateLimiter {

	private final StringRedisTemplate redisTemplate;

	RedisRateLimiter(StringRedisTemplate redisTemplate) {
		this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
	}

	@Override
	public RateLimitDecision check(String key, long limit, Duration window) {
		Objects.requireNonNull(key, "key must not be null");
		Objects.requireNonNull(window, "window must not be null");

		// Redis INCR는 카운터를 원자적으로 증가시키고 증가된 값을 반환한다.
		Long currentCount = redisTemplate.opsForValue().increment(key);
		if (currentCount == null) {
			throw new IllegalStateException("Redis INCR returned null");
		}
		// 첫 요청이 fixed window를 시작하므로 만료 시간은 한 번만 설정한다.
		if (currentCount == 1L) {
			redisTemplate.expire(key, window.toSeconds(), TimeUnit.SECONDS);
		}
		if (currentCount <= limit) {
			return RateLimitDecision.allowed(currentCount, limit);
		}

		// 제한을 초과한 요청에는 남은 TTL을 retry-after 기준으로 내려준다.
		Long ttlSeconds = redisTemplate.getExpire(key);
		if (ttlSeconds == null || ttlSeconds < 0) {
			redisTemplate.expire(key, window.toSeconds(), TimeUnit.SECONDS);
			return RateLimitDecision.rejected(currentCount, limit, window);
		}

		return RateLimitDecision.rejected(currentCount, limit, Duration.ofSeconds(ttlSeconds));
	}
}
