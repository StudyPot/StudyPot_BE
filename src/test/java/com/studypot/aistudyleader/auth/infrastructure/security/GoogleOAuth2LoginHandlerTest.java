package com.studypot.aistudyleader.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studypot.aistudyleader.auth.service.AuthSessionMetadata;
import com.studypot.aistudyleader.auth.service.AuthSessionService;
import com.studypot.aistudyleader.auth.service.AuthTokenResult;
import com.studypot.aistudyleader.auth.service.AuthenticatedUser;
import com.studypot.aistudyleader.auth.service.GoogleOAuthProfile;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

class GoogleOAuth2LoginHandlerTest {

	@Test
	void successHandlerIssuesHttpOnlyTokenCookiesAndRedirectsWithoutTokenQueryValues() throws Exception {
		AuthSessionService authSessionService = mock(AuthSessionService.class);
		when(authSessionService.loginWithGoogleProfile(any(GoogleOAuthProfile.class), any(AuthSessionMetadata.class)))
			.thenReturn(tokenResult());
		AuthProperties properties = properties();
		GoogleOAuth2LoginSuccessHandler handler = new GoogleOAuth2LoginSuccessHandler(
			provider(authSessionService),
			new AuthTokenCookieIssuer(properties),
			properties
		);
		MockHttpServletRequest request = request();
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.onAuthenticationSuccess(request, response, googleAuthentication());

		verify(authSessionService).loginWithGoogleProfile(
			argThat(profile ->
				profile.providerUserId().equals("google-123")
					&& profile.email().value().equals("member@example.com")
					&& profile.emailVerified()
					&& "Study Member".equals(profile.name())
					&& "https://cdn.example.com/member.png".equals(profile.picture())
					&& profile.scope().contains("openid")
					&& profile.scope().contains("email")
					&& profile.scope().contains("profile")),
			any(AuthSessionMetadata.class)
		);
		assertThat(response.getStatus()).isEqualTo(302);
		assertThat(response.getHeader(HttpHeaders.LOCATION))
			.isEqualTo("https://frontend.studypot.local/auth/success")
			.doesNotContain("access")
			.doesNotContain("refresh")
			.doesNotContain("token");
		assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
			.hasSize(2)
			.anySatisfy(cookie -> assertThat(cookie)
				.contains("studypot_access_token=access-token")
				.contains("HttpOnly")
				.contains("Secure")
				.contains("SameSite=Lax"))
			.anySatisfy(cookie -> assertThat(cookie)
				.contains("studypot_refresh_token=refresh-token")
				.contains("HttpOnly")
				.contains("Secure")
				.contains("SameSite=Lax"));
		assertThat(request.getSession(false)).isNull();
	}

	@Test
	void successHandlerDoesNotHideUnexpectedIllegalArgumentException() {
		AuthSessionService authSessionService = mock(AuthSessionService.class);
		when(authSessionService.loginWithGoogleProfile(any(GoogleOAuthProfile.class), any(AuthSessionMetadata.class)))
			.thenThrow(new IllegalArgumentException("provider bug"));
		AuthProperties properties = properties();
		GoogleOAuth2LoginSuccessHandler handler = new GoogleOAuth2LoginSuccessHandler(
			provider(authSessionService),
			new AuthTokenCookieIssuer(properties),
			properties
		);

		assertThatThrownBy(() -> handler.onAuthenticationSuccess(
				request(),
				new MockHttpServletResponse(),
				googleAuthentication()
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("provider bug");
	}

	@Test
	void failureHandlerRedirectsToFrontendFailureAndDoesNotIssueTokenCookies() throws Exception {
		GoogleOAuth2LoginFailureHandler handler = new GoogleOAuth2LoginFailureHandler(properties());
		MockHttpServletRequest request = request();
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.onAuthenticationFailure(
			request,
			response,
			new OAuth2AuthenticationException(new OAuth2Error("invalid_state"))
		);

		assertThat(response.getStatus()).isEqualTo(302);
		assertThat(response.getHeader(HttpHeaders.LOCATION))
			.isEqualTo("https://frontend.studypot.local/auth/failure");
		assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
			.noneSatisfy(cookie -> assertThat(cookie)
				.contains("studypot_access_token="))
			.noneSatisfy(cookie -> assertThat(cookie)
				.contains("studypot_refresh_token="));
		assertThat(request.getSession(false)).isNull();
	}

	private static OAuth2AuthenticationToken googleAuthentication() {
		Map<String, Object> attributes = Map.of(
			"sub", "google-123",
			"email", "member@example.com",
			"email_verified", true,
			"name", "Study Member",
			"picture", "https://cdn.example.com/member.png"
		);
		List<SimpleGrantedAuthority> scopeAuthorities = List.of(
			new SimpleGrantedAuthority("SCOPE_openid"),
			new SimpleGrantedAuthority("SCOPE_email"),
			new SimpleGrantedAuthority("SCOPE_profile")
		);
		List<GrantedAuthority> authorities = List.of(
			new OAuth2UserAuthority(attributes),
			scopeAuthorities.get(0),
			scopeAuthorities.get(1),
			scopeAuthorities.get(2)
		);
		DefaultOAuth2User principal = new DefaultOAuth2User(authorities, attributes, "sub");
		return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
	}

	private static MockHttpServletRequest request() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSecure(true);
		request.addHeader(HttpHeaders.USER_AGENT, "JUnit");
		request.setRemoteAddr("127.0.0.1");
		request.setSession(new MockHttpSession());
		return request;
	}

	private static AuthTokenResult tokenResult() {
		return new AuthTokenResult(
			"access-token",
			"refresh-token",
			"Bearer",
			900,
			new AuthenticatedUser(
				UUID.fromString("018f0000-0000-7000-8000-000000000201"),
				"member@example.com",
				"Study Member",
				null
			)
		);
	}

	private static ObjectProvider<AuthSessionService> provider(AuthSessionService service) {
		StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
		beanFactory.addBean("authSessionService", service);
		return beanFactory.getBeanProvider(AuthSessionService.class);
	}

	private static AuthProperties properties() {
		return new AuthProperties(
			new AuthProperties.Jwt("01234567890123456789012345678901", "https://api.studypot.local", Duration.ofMinutes(15)),
			Duration.ofDays(30),
			new AuthProperties.Cookie("studypot_access_token", "studypot_refresh_token", null, "/", true, "Lax"),
			new AuthProperties.OAuth2(
				"https://api.studypot.local/api/login/oauth2/code/google",
				URI.create("https://frontend.studypot.local/auth/success"),
				URI.create("https://frontend.studypot.local/auth/failure")
			)
		);
	}
}
