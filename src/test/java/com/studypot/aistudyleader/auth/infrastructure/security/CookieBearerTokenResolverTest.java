package com.studypot.aistudyleader.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

class CookieBearerTokenResolverTest {

	private final AuthTokenCookieIssuer cookieIssuer = mock(AuthTokenCookieIssuer.class);
	private final BearerTokenResolver resolver = new CookieBearerTokenResolver(cookieIssuer);

	@Test
	void resolvesAccessTokenQueryParamForAiConversationStream() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET",
			"/api/v1/ai-conversations/018f0000-0000-7000-8000-000000000001/stream");
		request.setParameter("access_token", "jwt-ai-stream");

		assertThat(resolver.resolve(request)).isEqualTo("jwt-ai-stream");
	}

	@Test
	void resolvesAccessTokenQueryParamForNotificationStream() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me/notifications/stream");
		request.setParameter("access_token", "jwt-noti-stream");

		assertThat(resolver.resolve(request)).isEqualTo("jwt-noti-stream");
	}

	@Test
	void ignoresAccessTokenQueryParamForNonStreamEndpoint() {
		when(cookieIssuer.accessToken(any())).thenReturn(Optional.empty());
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
		request.setParameter("access_token", "jwt-should-be-ignored");

		assertThat(resolver.resolve(request)).isNull();
	}

	@Test
	void prefersAuthorizationHeaderOverQueryParamOnStream() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET",
			"/api/v1/ai-conversations/018f0000-0000-7000-8000-000000000001/stream");
		request.addHeader("Authorization", "Bearer header-token");
		request.setParameter("access_token", "query-token");

		assertThat(resolver.resolve(request)).isEqualTo("header-token");
	}

	@Test
	void fallsBackToCookieWhenNoHeaderOrStreamQueryToken() {
		when(cookieIssuer.accessToken(any())).thenReturn(Optional.of("cookie-token"));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/groups/summary");

		assertThat(resolver.resolve(request)).isEqualTo("cookie-token");
	}
}
