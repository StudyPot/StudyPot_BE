package com.studypot.aistudyleader.identity.domain;

public enum OAuthProvider {
	GOOGLE;

	public static OAuthProvider fromPersistence(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("OAuth provider from persistence must not be blank");
		}
		try {
			return OAuthProvider.valueOf(value.strip());
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("Unsupported OAuth provider from persistence: " + value.strip(), exception);
		}
	}

	public String liveKey(String providerUserId) {
		String normalizedProviderUserId = requireProviderUserId(providerUserId);
		return name() + ":" + normalizedProviderUserId;
	}

	private static String requireProviderUserId(String providerUserId) {
		if (providerUserId == null || providerUserId.isBlank()) {
			throw new IllegalArgumentException("providerUserId must not be blank");
		}
		String normalized = providerUserId.strip();
		if (normalized.contains(":")) {
			throw new IllegalArgumentException("providerUserId must not contain ':'");
		}
		return normalized;
	}
}
