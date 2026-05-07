package com.studypot.aistudyleader.identity.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "studypot.auth")
public record AuthProperties(Jwt jwt, Duration refreshTokenTtl) {

	private static final Duration DEFAULT_ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
	private static final Duration DEFAULT_REFRESH_TOKEN_TTL = Duration.ofDays(30);
	private static final String DEFAULT_ISSUER = "https://api.studypot.example";

	public AuthProperties {
		jwt = jwt == null ? new Jwt(null, DEFAULT_ISSUER, DEFAULT_ACCESS_TOKEN_TTL) : jwt;
		refreshTokenTtl = positiveOrDefault(refreshTokenTtl, DEFAULT_REFRESH_TOKEN_TTL);
	}

	public record Jwt(String secret, String issuer, Duration accessTokenTtl) {

		public Jwt {
			issuer = issuer == null || issuer.isBlank() ? DEFAULT_ISSUER : issuer.strip();
			accessTokenTtl = positiveOrDefault(accessTokenTtl, DEFAULT_ACCESS_TOKEN_TTL);
		}
	}

	private static Duration positiveOrDefault(Duration value, Duration defaultValue) {
		if (value == null || value.isZero() || value.isNegative()) {
			return defaultValue;
		}
		return value;
	}
}
