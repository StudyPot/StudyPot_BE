package com.studypot.aistudyleader.global.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;

class BrowserCsrfProtectionFilterTest {

	private AccessDeniedHandler accessDeniedHandler;
	private FilterChain filterChain;
	private HttpServletRequest request;
	private HttpServletResponse response;
	private BrowserCsrfProtectionFilter filter;

	@BeforeEach
	void setUp() {
		accessDeniedHandler = mock(AccessDeniedHandler.class);
		filterChain = mock(FilterChain.class);
		request = mock(HttpServletRequest.class);
		response = mock(HttpServletResponse.class);
		filter = new BrowserCsrfProtectionFilter(accessDeniedHandler, req -> true);
	}

	@Test
	void requestPassesThroughWhenCsrfNotRequired() throws Exception {
		BrowserCsrfProtectionFilter notRequiredFilter =
			new BrowserCsrfProtectionFilter(accessDeniedHandler, req -> false);

		notRequiredFilter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(accessDeniedHandler, never()).handle(any(), any(), any());
	}

	@Test
	void requestPassesThroughWhenXsrfCookieAndHeaderMatch() throws Exception {
		when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("XSRF-TOKEN", "abc123")});
		when(request.getHeader("X-XSRF-TOKEN")).thenReturn("abc123");

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(accessDeniedHandler, never()).handle(any(), any(), any());
	}

	@Test
	void requestIsRejectedWhenXsrfCookiePresentButHeaderMissing() throws Exception {
		when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("XSRF-TOKEN", "abc123")});
		when(request.getHeader("X-XSRF-TOKEN")).thenReturn(null);
		when(request.getHeader("X-CSRF-TOKEN")).thenReturn(null);
		when(request.getParameter("_csrf")).thenReturn(null);

		filter.doFilter(request, response, filterChain);

		verify(filterChain, never()).doFilter(any(), any());
		verify(accessDeniedHandler).handle(any(), any(), any());
	}

	@Test
	void requestIsRejectedWhenNoCookieExists() throws Exception {
		when(request.getCookies()).thenReturn(null);

		filter.doFilter(request, response, filterChain);

		verify(filterChain, never()).doFilter(any(), any());
		verify(accessDeniedHandler).handle(any(), any(), any());
	}

	@Test
	void requestPassesThroughWhenTrustedCorsOriginHasCsrfHeaderWithoutXsrfCookie() throws Exception {
		BrowserCsrfProtectionFilter trustedOriginFilter = new BrowserCsrfProtectionFilter(
			accessDeniedHandler,
			req -> true,
			req -> corsConfiguration("https://studypot.netlify.app", true)
		);
		when(request.getCookies()).thenReturn(null);
		when(request.getHeader("X-XSRF-TOKEN")).thenReturn("bootstrapped-token");
		when(request.getHeader(HttpHeaders.ORIGIN)).thenReturn("https://studypot.netlify.app");

		trustedOriginFilter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(accessDeniedHandler, never()).handle(any(), any(), any());
	}

	@Test
	void requestIsRejectedWhenUntrustedOriginHasCsrfHeaderWithoutXsrfCookie() throws Exception {
		BrowserCsrfProtectionFilter trustedOriginFilter = new BrowserCsrfProtectionFilter(
			accessDeniedHandler,
			req -> true,
			req -> corsConfiguration("https://studypot.netlify.app", true)
		);
		when(request.getCookies()).thenReturn(null);
		when(request.getHeader("X-XSRF-TOKEN")).thenReturn("attacker-token");
		when(request.getHeader(HttpHeaders.ORIGIN)).thenReturn("https://evil.example");

		trustedOriginFilter.doFilter(request, response, filterChain);

		verify(filterChain, never()).doFilter(any(), any());
		verify(accessDeniedHandler).handle(any(), any(), any());
	}

	@Test
	void requestIsRejectedWhenCorsOriginIsAllowedWithoutCredentials() throws Exception {
		BrowserCsrfProtectionFilter trustedOriginFilter = new BrowserCsrfProtectionFilter(
			accessDeniedHandler,
			req -> true,
			req -> corsConfiguration("https://studypot.netlify.app", false)
		);
		when(request.getCookies()).thenReturn(null);
		when(request.getHeader("X-XSRF-TOKEN")).thenReturn("bootstrapped-token");
		when(request.getHeader(HttpHeaders.ORIGIN)).thenReturn("https://studypot.netlify.app");

		trustedOriginFilter.doFilter(request, response, filterChain);

		verify(filterChain, never()).doFilter(any(), any());
		verify(accessDeniedHandler).handle(any(), any(), any());
	}

	@Test
	void requestPassesThroughWhenXCsrfTokenHeaderMatches() throws Exception {
		when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("XSRF-TOKEN", "abc123")});
		when(request.getHeader("X-XSRF-TOKEN")).thenReturn(null);
		when(request.getHeader("X-CSRF-TOKEN")).thenReturn("abc123");

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(accessDeniedHandler, never()).handle(any(), any(), any());
	}

	@Test
	void requestPassesThroughWhenCsrfParameterMatches() throws Exception {
		when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("XSRF-TOKEN", "abc123")});
		when(request.getHeader("X-XSRF-TOKEN")).thenReturn(null);
		when(request.getHeader("X-CSRF-TOKEN")).thenReturn(null);
		when(request.getParameter("_csrf")).thenReturn("abc123");

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(accessDeniedHandler, never()).handle(any(), any(), any());
	}

	@Test
	void constructorRejectsMissingRequirementMatcher() {
		assertThatNullPointerException()
			.isThrownBy(() -> new BrowserCsrfProtectionFilter(accessDeniedHandler, null))
			.withMessage("requirementMatcher must not be null");
	}

	private static CorsConfiguration corsConfiguration(String origin, boolean allowCredentials) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of(origin));
		configuration.setAllowCredentials(allowCredentials);
		return configuration;
	}
}
