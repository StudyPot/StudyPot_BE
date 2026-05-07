package com.studypot.aistudyleader.identity.application;

import com.studypot.aistudyleader.identity.domain.EmailAddress;
import java.time.Instant;
import java.util.Objects;

public record GoogleOAuthProfile(
	String providerUserId,
	EmailAddress email,
	boolean emailVerified,
	String name,
	String picture,
	Instant tokenExpiresAt,
	String scope
) {

	public GoogleOAuthProfile {
		if (providerUserId == null || providerUserId.isBlank()) {
			throw new IllegalArgumentException("providerUserId must not be blank");
		}
		providerUserId = providerUserId.strip();
		email = Objects.requireNonNull(email, "email must not be null");
		name = blankToNull(name);
		picture = blankToNull(picture);
		scope = blankToNull(scope);
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}
}
