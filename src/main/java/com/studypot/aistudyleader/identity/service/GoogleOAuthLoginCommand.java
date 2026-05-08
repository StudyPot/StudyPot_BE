package com.studypot.aistudyleader.identity.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;

public final class GoogleOAuthLoginCommand {

	private final String authorizationCode;
	private final URI redirectUri;
	private final String codeVerifier;

	public GoogleOAuthLoginCommand(String authorizationCode, String redirectUri, String codeVerifier) {
		this.authorizationCode = requireNonBlank("authorizationCode", authorizationCode);
		this.redirectUri = requireRedirectUri(redirectUri);
		this.codeVerifier = blankToNull(codeVerifier);
	}

	public String authorizationCode() {
		return authorizationCode;
	}

	public URI redirectUri() {
		return redirectUri;
	}

	public Optional<String> codeVerifier() {
		return Optional.ofNullable(codeVerifier);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		return other instanceof GoogleOAuthLoginCommand command
			&& authorizationCode.equals(command.authorizationCode)
			&& redirectUri.equals(command.redirectUri)
			&& Objects.equals(codeVerifier, command.codeVerifier);
	}

	@Override
	public int hashCode() {
		return Objects.hash(authorizationCode, redirectUri, codeVerifier);
	}

	@Override
	public String toString() {
		return "GoogleOAuthLoginCommand[authorizationCode=****, redirectUri=%s, codeVerifier=%s]"
			.formatted(redirectUri, codeVerifier == null ? "null" : "****");
	}

	private static String requireNonBlank(String field, String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value.strip();
	}

	private static URI requireRedirectUri(String value) {
		String normalized = requireNonBlank("redirectUri", value);
		try {
			URI uri = new URI(normalized);
			String scheme = uri.getScheme();
			if (!uri.isAbsolute() || scheme == null || (!scheme.equals("https") && !scheme.equals("http"))) {
				throw new IllegalArgumentException("redirectUri must be an absolute http(s) URI");
			}
			return uri;
		} catch (URISyntaxException exception) {
			throw new IllegalArgumentException("redirectUri must be a valid URI", exception);
		}
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}
}
