package com.studypot.aistudyleader.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Optional;

/**
 * Service-facing port for writing and reading authentication cookies.
 * Implementations own cookie names and security attributes such as HttpOnly,
 * Secure, SameSite, Path, Domain, and Max-Age. Values are raw token strings;
 * this port does not encrypt or sign values beyond the token format itself.
 * Cookie-backed OAuth login and refresh-token reads are approved by
 * CR-20260508-oauth2-cookie-login and ADR-20260508-oauth2-cookie-login.
 */
public interface AuthTokenCookiePort {

	/**
	 * Adds access-token and refresh-token cookies for a newly issued token pair.
	 *
	 * @param response servlet response receiving Set-Cookie headers; must not be null
	 * @param result issued token pair; must not be null and must contain valid token values
	 */
	void addTokenCookies(HttpServletResponse response, AuthTokenResult result);

	/**
	 * Adds expired Set-Cookie headers for both application token cookies.
	 *
	 * @param response servlet response receiving deletion headers; must not be null
	 */
	void clearTokenCookies(HttpServletResponse response);

	/**
	 * Adds a short-lived temporary OAuth cookie, typically for framework hand-off
	 * state. Negative or zero durations are interpreted by the implementation as
	 * a session cookie unless documented otherwise.
	 *
	 * @param response servlet response receiving Set-Cookie headers; must not be null
	 * @param name cookie name; must identify an allowed temporary cookie
	 * @param value cookie value; must not be null for active cookies
	 * @param maxAge cookie lifetime
	 */
	void addTemporaryCookie(HttpServletResponse response, String name, String value, Duration maxAge);

	/**
	 * Adds an expired Set-Cookie header for the named cookie.
	 *
	 * @param response servlet response receiving deletion headers; must not be null
	 * @param name cookie name to clear; must not be blank
	 */
	void clearCookie(HttpServletResponse response, String name);

	/**
	 * Reads the application refresh-token cookie from the request.
	 *
	 * @param request servlet request containing cookies; must not be null
	 * @return refresh token cookie value when present and non-blank
	 */
	Optional<String> refreshToken(HttpServletRequest request);
}
