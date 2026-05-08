package com.studypot.aistudyleader.identity.domain;

public enum OAuthProvider {
	GOOGLE;

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
