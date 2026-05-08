package com.studypot.aistudyleader.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.web.filter.OncePerRequestFilter;

class BrowserCsrfProtectionFilter extends OncePerRequestFilter {

	private static final String XSRF_COOKIE = "XSRF-TOKEN";
	private static final String X_XSRF_TOKEN = "X-XSRF-TOKEN";
	private static final String X_CSRF_TOKEN = "X-CSRF-TOKEN";
	private static final Set<String> CSRF_SAFE_METHODS = Set.of("GET", "HEAD", "TRACE", "OPTIONS");

	private final AccessDeniedHandler accessDeniedHandler;
	private final CsrfRequirementMatcher requirementMatcher;

	BrowserCsrfProtectionFilter(AccessDeniedHandler accessDeniedHandler, CsrfRequirementMatcher requirementMatcher) {
		this.accessDeniedHandler = accessDeniedHandler == null ? new AccessDeniedHandlerImpl() : accessDeniedHandler;
		this.requirementMatcher = requirementMatcher;
	}

	@FunctionalInterface
	interface CsrfRequirementMatcher {
		boolean requiresCsrfProtection(HttpServletRequest request);
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		if (!requirementMatcher.requiresCsrfProtection(request) || xsrfTokenMatches(request)) {
			filterChain.doFilter(request, response);
			return;
		}
		accessDeniedHandler.handle(request, response, new AccessDeniedException("CSRF token is required."));
	}

	private static boolean xsrfTokenMatches(HttpServletRequest request) {
		String expected = cookieValue(request, XSRF_COOKIE);
		String actual = headerValue(request, X_XSRF_TOKEN);
		if (actual == null) {
			actual = headerValue(request, X_CSRF_TOKEN);
		}
		if (actual == null) {
			actual = parameterValue(request, "_csrf");
		}
		return expected != null
			&& actual != null
			&& MessageDigest.isEqual(
				expected.getBytes(StandardCharsets.UTF_8),
				actual.getBytes(StandardCharsets.UTF_8)
			);
	}

	private static String cookieValue(HttpServletRequest request, String name) {
		if (request.getCookies() == null) {
			return null;
		}
		for (Cookie cookie : request.getCookies()) {
			if (cookie.getName().equals(name) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private static String headerValue(HttpServletRequest request, String name) {
		String value = request.getHeader(name);
		return value == null || value.isBlank() ? null : value;
	}

	private static String parameterValue(HttpServletRequest request, String name) {
		String value = request.getParameter(name);
		return value == null || value.isBlank() ? null : value;
	}
}
