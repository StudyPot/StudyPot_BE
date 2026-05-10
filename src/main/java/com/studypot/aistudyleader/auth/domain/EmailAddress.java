package com.studypot.aistudyleader.auth.domain;

import java.util.Locale;

public record EmailAddress(String value) {

	private static final int MAX_LENGTH = 320;

	public EmailAddress {
		value = canonicalize(value);
		if (value.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("email must be 320 characters or less");
		}
		int at = value.indexOf('@');
		if (at <= 0 || at == value.length() - 1 || value.indexOf('@', at + 1) >= 0) {
			throw new IllegalArgumentException("email must be valid");
		}
	}

	public static EmailAddress from(String value) {
		return new EmailAddress(value);
	}

	public String liveKey() {
		return value;
	}

	private static String canonicalize(String value) {
		if (value == null) {
			throw new IllegalArgumentException("email must not be null");
		}
		String canonical = value.strip().toLowerCase(Locale.ROOT);
		if (canonical.isBlank()) {
			throw new IllegalArgumentException("email must not be blank");
		}
		return canonical;
	}
}
