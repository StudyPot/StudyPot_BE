package com.studypot.aistudyleader.identity.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Optional;

public interface AuthTokenCookiePort {

	void addTokenCookies(HttpServletResponse response, AuthTokenResult result);

	void clearTokenCookies(HttpServletResponse response);

	void addTemporaryCookie(HttpServletResponse response, String name, String value, Duration maxAge);

	void clearCookie(HttpServletResponse response, String name);

	Optional<String> accessToken(HttpServletRequest request);

	Optional<String> refreshToken(HttpServletRequest request);

	Optional<String> cookieValue(HttpServletRequest request, String name);
}
