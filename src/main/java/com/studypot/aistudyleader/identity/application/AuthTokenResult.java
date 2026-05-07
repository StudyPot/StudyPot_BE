package com.studypot.aistudyleader.identity.application;

public record AuthTokenResult(
	String accessToken,
	String refreshToken,
	String tokenType,
	long expiresIn,
	AuthenticatedUser user
) {
}
