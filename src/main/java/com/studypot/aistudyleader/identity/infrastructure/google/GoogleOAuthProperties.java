package com.studypot.aistudyleader.identity.infrastructure.google;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "studypot.oauth.google")
public record GoogleOAuthProperties(
	String clientId,
	String clientSecret,
	String tokenUri,
	String userinfoUri
) {

	private static final String DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token";
	private static final String DEFAULT_USERINFO_URI = "https://openidconnect.googleapis.com/v1/userinfo";

	public GoogleOAuthProperties {
		clientId = blankToNull(clientId);
		clientSecret = blankToNull(clientSecret);
		tokenUri = defaultIfBlank(tokenUri, DEFAULT_TOKEN_URI);
		userinfoUri = defaultIfBlank(userinfoUri, DEFAULT_USERINFO_URI);
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
}
