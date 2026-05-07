package com.studypot.aistudyleader.identity.infrastructure.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.function.Supplier;

class SecureRefreshTokenGenerator implements Supplier<String> {

	private static final int TOKEN_BYTES = 32;

	private final SecureRandom secureRandom = new SecureRandom();

	@Override
	public String get() {
		byte[] bytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
