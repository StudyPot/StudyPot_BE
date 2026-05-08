package com.studypot.aistudyleader.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class RefreshTokenHasher {

	private RefreshTokenHasher() {
	}

	public static String sha256Hex(String rawToken) {
		if (rawToken == null || rawToken.strip().isBlank()) {
			throw new RefreshTokenRejectedException("refresh token must not be blank.");
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available.", exception);
		}
	}
}
