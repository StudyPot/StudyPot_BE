package com.studypot.aistudyleader.identity.infrastructure.google;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "studypot.oauth.google")
public record GoogleOAuthProperties(
	String clientId,
	String clientSecret,
	String tokenUri,
	String userinfoUri,
	String authorizationUri,
	List<String> scopes,
	Duration connectTimeout,
	Duration readTimeout
) {

	private static final String DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token";
	private static final String DEFAULT_USERINFO_URI = "https://openidconnect.googleapis.com/v1/userinfo";
	private static final String DEFAULT_AUTHORIZATION_URI = "https://accounts.google.com/o/oauth2/v2/auth";
	private static final List<String> DEFAULT_SCOPES = List.of("openid", "email", "profile");
	private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);

	public GoogleOAuthProperties {
		clientId = blankToNull(clientId);
		clientSecret = blankToNull(clientSecret);
		tokenUri = requireHttpsUri(defaultIfBlank(tokenUri, DEFAULT_TOKEN_URI), "token-uri");
		userinfoUri = requireHttpsUri(defaultIfBlank(userinfoUri, DEFAULT_USERINFO_URI), "userinfo-uri");
		authorizationUri = requireHttpsUri(defaultIfBlank(authorizationUri, DEFAULT_AUTHORIZATION_URI), "authorization-uri");
		scopes = normalizeScopes(scopes);
		connectTimeout = positiveOrDefault(connectTimeout, DEFAULT_CONNECT_TIMEOUT);
		readTimeout = positiveOrDefault(readTimeout, DEFAULT_READ_TIMEOUT);
	}

	@Override
	public String toString() {
		return "GoogleOAuthProperties[clientId=%s, clientSecret=****, tokenUri=%s, userinfoUri=%s, authorizationUri=%s, scopes=%s, connectTimeout=%s, readTimeout=%s]"
			.formatted(clientId, tokenUri, userinfoUri, authorizationUri, scopes, connectTimeout, readTimeout);
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

	private static List<String> normalizeScopes(List<String> scopes) {
		if (scopes == null || scopes.isEmpty()) {
			return DEFAULT_SCOPES;
		}
		List<String> normalized = scopes.stream()
			.filter(value -> value != null && !value.isBlank())
			.map(String::strip)
			.toList();
		return normalized.isEmpty() ? DEFAULT_SCOPES : List.copyOf(normalized);
	}

	private static Duration positiveOrDefault(Duration value, Duration defaultValue) {
		if (value == null || value.isZero() || value.isNegative()) {
			return defaultValue;
		}
		return value;
	}

	private static String requireHttpsUri(String value, String property) {
		try {
			URI uri = new URI(value);
			if (!uri.isAbsolute() || uri.getScheme() == null || !uri.getScheme().equalsIgnoreCase("https")) {
				throw new IllegalArgumentException("studypot.oauth.google." + property + " must be an absolute HTTPS URI.");
			}
			return value;
		} catch (URISyntaxException exception) {
			throw new IllegalArgumentException("studypot.oauth.google." + property + " must be a valid URI.", exception);
		}
	}
}
