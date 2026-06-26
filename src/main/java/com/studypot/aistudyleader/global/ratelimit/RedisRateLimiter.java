package com.studypot.aistudyleader.global.ratelimit;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

final class RedisRateLimiter implements RateLimiter {

	private static final RedisScript<Long> RATE_LIMIT_SCRIPT = RedisScript.of("""
		local current = redis.call('INCR', KEYS[1])
		if current == 1 then
			redis.call('PEXPIRE', KEYS[1], ARGV[1])
		end
		return current
		""", Long.class);

	private final StringRedisTemplate redisTemplate;

	RedisRateLimiter(StringRedisTemplate redisTemplate) {
		this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
	}

	@Override
	public RateLimitDecision check(String key, long limit, Duration window) {
		Objects.requireNonNull(key, "key must not be null");
		Objects.requireNonNull(window, "window must not be null");
		if (limit <= 0) {
			throw new IllegalArgumentException("limit must be positive");
		}
		if (window.isZero() || window.isNegative()) {
			throw new IllegalArgumentException("window must be positive");
		}

		// Lua 스크립트로 INCR와 PEXPIRE를 한 번에 실행해 첫 요청 race를 막는다.
		Long currentCount = redisTemplate.execute(
			RATE_LIMIT_SCRIPT,
			Collections.singletonList(key),
			Long.toString(window.toMillis())
		);
		if (currentCount == null) {
			throw new IllegalStateException("Redis rate limit script returned null");
		}
		if (currentCount <= limit) {
			return RateLimitDecision.allowed(currentCount, limit);
		}

		// 제한을 초과한 요청에는 남은 TTL을 retry-after 기준으로 내려준다.
		Long ttlMillis = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
		if (ttlMillis == null || ttlMillis <= 0) {
			redisTemplate.delete(key);
			return RateLimitDecision.rejected(currentCount, limit, window);
		}

		return RateLimitDecision.rejected(currentCount, limit, Duration.ofMillis(ttlMillis));
	}

	@Override
	public RateLimitSnapshot peek(String key) {
		Objects.requireNonNull(key, "key must not be null");
		String raw = redisTemplate.opsForValue().get(key);
		if (raw == null) {
			return new RateLimitSnapshot(0, Duration.ZERO);
		}
		long count;
		try {
			count = Long.parseLong(raw.trim());
		} catch (NumberFormatException exception) {
			count = 0;
		}
		Long ttlMillis = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
		Duration reset = ttlMillis == null || ttlMillis <= 0 ? Duration.ZERO : Duration.ofMillis(ttlMillis);
		return new RateLimitSnapshot(count, reset);
	}
}
