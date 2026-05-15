package com.studypot.aistudyleader.global.ratelimit;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
class RateLimitConfiguration {

	@Bean
	@ConditionalOnMissingBean(RateLimiter.class)
	RateLimiter rateLimiter(RateLimitProperties properties, ObjectProvider<StringRedisTemplate> redisTemplate) {
		if (!properties.enabled()) {
			return new NoOpRateLimiter();
		}
		return new RedisRateLimiter(redisTemplate.getIfAvailable(() -> {
			throw new IllegalStateException("Redis rate limit is enabled but StringRedisTemplate is not configured.");
		}));
	}
}
