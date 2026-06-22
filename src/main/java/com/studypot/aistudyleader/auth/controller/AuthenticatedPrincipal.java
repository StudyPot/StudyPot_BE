package com.studypot.aistudyleader.auth.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

final class AuthenticatedPrincipal {

	private AuthenticatedPrincipal() {
	}

	static UUID userId(Jwt jwt) {
		if (jwt == null || jwt.getSubject() == null) {
			throw new AuthSessionRejectedException("authenticated user is required.");
		}
		try {
			return UUID.fromString(jwt.getSubject());
		} catch (IllegalArgumentException exception) {
			throw new AuthSessionRejectedException("authenticated user is invalid.");
		}
	}
}
