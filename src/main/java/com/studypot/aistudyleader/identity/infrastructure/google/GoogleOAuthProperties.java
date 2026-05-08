package com.studypot.aistudyleader.identity.infrastructure.google;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "studypot.oauth.google")
public record GoogleOAuthProperties(
	String clientId,
	String clientSecret,
	String tokenUri,
	String userinfoUri,
	String authorizationUri,
	List<String> scopes
) {

	private static final String DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token";
	private static final String DEFAULT_USERINFO_URI = "https://openidconnect.googleapis.com/v1/userinfo";
	private static final String DEFAULT_AUTHORIZATION_URI = "https://accounts.google.com/o/oauth2/v2/auth";
	private static final List<String> DEFAULT_SCOPES = List.of("openid", "email", "profile");

	public GoogleOAuthProperties {
		clientId = blankToNull(clientId);
		clientSecret = blankToNull(clientSecret);
		tokenUri = defaultIfBlank(tokenUri, DEFAULT_TOKEN_URI);
		userinfoUri = defaultIfBlank(userinfoUri, DEFAULT_USERINFO_URI);
		authorizationUri = defaultIfBlank(authorizationUri, DEFAULT_AUTHORIZATION_URI);
		scopes = normalizeScopes(scopes);
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
}
