package com.studypot.aistudyleader.identity.service;

import java.time.Instant;
import java.util.Objects;

public record IssuedAccessToken(String token, Instant expiresAt) {

	public IssuedAccessToken {
		Objects.requireNonNull(token, "token must not be null");
		Objects.requireNonNull(expiresAt, "expiresAt must not be null");
		if (token.isBlank()) {
			throw new IllegalArgumentException("token must not be blank");
		}
	}
}
