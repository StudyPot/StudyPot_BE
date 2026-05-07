package com.studypot.aistudyleader.identity.service;

public record AuthTokenResult(
	String accessToken,
	String refreshToken,
	String tokenType,
	long expiresIn,
	AuthenticatedUser user
) {
}
