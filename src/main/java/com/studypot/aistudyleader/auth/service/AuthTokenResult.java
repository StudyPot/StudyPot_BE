package com.studypot.aistudyleader.auth.service;

public record AuthTokenResult(
	String accessToken,
	String refreshToken,
	String tokenType,
	long expiresIn,
	AuthenticatedUser user
) {
}
