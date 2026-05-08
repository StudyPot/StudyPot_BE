package com.studypot.aistudyleader.identity.infrastructure.security;

import com.studypot.aistudyleader.identity.service.AuthTokenCookiePort;
import com.studypot.aistudyleader.identity.service.AuthTokenResult;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

@RequiredArgsConstructor
public class AuthTokenCookieIssuer implements AuthTokenCookiePort {

	private final AuthProperties properties;

	@Override
	public void addTokenCookies(HttpServletResponse response, AuthTokenResult result) {
		addCookie(
			response,
			properties.cookie().accessTokenName(),
			result.accessToken(),
			Duration.ofSeconds(result.expiresIn())
		);
		addCookie(response, properties.cookie().refreshTokenName(), result.refreshToken(), properties.refreshTokenTtl());
	}

	@Override
	public void clearTokenCookies(HttpServletResponse response) {
		clearCookie(response, properties.cookie().accessTokenName());
		clearCookie(response, properties.cookie().refreshTokenName());
	}

	@Override
	public void addTemporaryCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
		addCookie(response, name, value, maxAge);
	}

	@Override
	public void clearCookie(HttpServletResponse response, String name) {
		response.addHeader(HttpHeaders.SET_COOKIE, baseCookie(name, "")
			.maxAge(Duration.ZERO)
			.build()
			.toString());
	}

	@Override
	public Optional<String> accessToken(HttpServletRequest request) {
		return cookieValue(request, properties.cookie().accessTokenName());
	}

	@Override
	public Optional<String> refreshToken(HttpServletRequest request) {
		return cookieValue(request, properties.cookie().refreshTokenName());
	}

	@Override
	public Optional<String> cookieValue(HttpServletRequest request, String name) {
		if (request.getCookies() == null) {
			return Optional.empty();
		}
		return Arrays.stream(request.getCookies())
			.filter(cookie -> cookie.getName().equals(name))
			.map(Cookie::getValue)
			.filter(value -> value != null && !value.isBlank())
			.findFirst();
	}

	private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
		response.addHeader(HttpHeaders.SET_COOKIE, baseCookie(name, value)
			.maxAge(maxAgeSeconds(maxAge))
			.build()
			.toString());
	}

	private ResponseCookie.ResponseCookieBuilder baseCookie(String name, String value) {
		ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(properties.cookie().secure())
			.path(properties.cookie().path())
			.sameSite(properties.cookie().sameSite());
		if (properties.cookie().domain() != null) {
			cookie.domain(properties.cookie().domain());
		}
		return cookie;
	}

	private static Duration maxAgeSeconds(Duration maxAge) {
		if (maxAge == null || maxAge.isZero() || maxAge.isNegative()) {
			return Duration.ofSeconds(-1);
		}
		return Duration.ofSeconds(Math.min(maxAge.toSeconds(), Integer.MAX_VALUE));
	}
}
