package com.studypot.aistudyleader.identity.infrastructure.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "studypot.auth")
public record AuthProperties(Jwt jwt, Duration refreshTokenTtl, Cookie cookie, OAuth2 oauth2) {

	private static final Duration DEFAULT_ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
	private static final Duration DEFAULT_REFRESH_TOKEN_TTL = Duration.ofDays(30);
	private static final String DEFAULT_ISSUER = "https://api.studypot.example";
	private static final String DEFAULT_ACCESS_TOKEN_COOKIE = "studypot_access_token";
	private static final String DEFAULT_REFRESH_TOKEN_COOKIE = "studypot_refresh_token";
	private static final String DEFAULT_COOKIE_PATH = "/";
	private static final String DEFAULT_COOKIE_SAME_SITE = "Lax";
	private static final URI DEFAULT_FRONTEND_SUCCESS_URI = URI.create("http://localhost:3000/auth/success");
	private static final URI DEFAULT_FRONTEND_FAILURE_URI = URI.create("http://localhost:3000/auth/failure");

	public AuthProperties {
		jwt = jwt == null ? new Jwt(null, DEFAULT_ISSUER, DEFAULT_ACCESS_TOKEN_TTL) : jwt;
		refreshTokenTtl = positiveOrDefault(refreshTokenTtl, DEFAULT_REFRESH_TOKEN_TTL);
		cookie = cookie == null ? new Cookie(null, null, null, null, null, null) : cookie;
		oauth2 = oauth2 == null ? new OAuth2(null, null, null) : oauth2;
	}

	public record Jwt(String secret, String issuer, Duration accessTokenTtl) {

		public Jwt {
			issuer = issuer == null || issuer.isBlank() ? DEFAULT_ISSUER : issuer.strip();
			accessTokenTtl = positiveOrDefault(accessTokenTtl, DEFAULT_ACCESS_TOKEN_TTL);
		}
	}

	public record Cookie(
		String accessTokenName,
		String refreshTokenName,
		String domain,
		String path,
		Boolean secure,
		String sameSite
	) {

		public Cookie {
			accessTokenName = defaultIfBlank(accessTokenName, DEFAULT_ACCESS_TOKEN_COOKIE);
			refreshTokenName = defaultIfBlank(refreshTokenName, DEFAULT_REFRESH_TOKEN_COOKIE);
			domain = blankToNull(domain);
			path = defaultIfBlank(path, DEFAULT_COOKIE_PATH);
			secure = secure == null || secure;
			sameSite = defaultIfBlank(sameSite, DEFAULT_COOKIE_SAME_SITE);
		}
	}

	public record OAuth2(String backendCallbackUri, URI frontendSuccessUri, URI frontendFailureUri) {

		public OAuth2 {
			backendCallbackUri = validateOptionalHttpUri(backendCallbackUri, "backendCallbackUri");
			frontendSuccessUri = frontendSuccessUri == null ? DEFAULT_FRONTEND_SUCCESS_URI : frontendSuccessUri;
			frontendFailureUri = frontendFailureUri == null ? DEFAULT_FRONTEND_FAILURE_URI : frontendFailureUri;
		}
	}

	private static Duration positiveOrDefault(Duration value, Duration defaultValue) {
		if (value == null || value.isZero() || value.isNegative()) {
			return defaultValue;
		}
		return value;
	}

	private static String defaultIfBlank(String value, String defaultValue) {
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		return value.strip();
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}

	private static String validateOptionalHttpUri(String value, String field) {
		String normalized = blankToNull(value);
		if (normalized == null) {
			return null;
		}
		try {
			URI uri = new URI(normalized);
			String scheme = uri.getScheme();
			if (!uri.isAbsolute() || scheme == null || (!scheme.equals("https") && !scheme.equals("http"))) {
				throw new IllegalArgumentException("studypot.auth.oauth2." + field + " must be an absolute http(s) URI.");
			}
			return normalized;
		} catch (URISyntaxException exception) {
			throw new IllegalArgumentException("studypot.auth.oauth2." + field + " must be a valid URI.", exception);
		}
	}
}
