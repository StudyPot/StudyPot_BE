package com.studypot.aistudyleader.identity.infrastructure.security;

import com.studypot.aistudyleader.identity.domain.EmailAddress;
import com.studypot.aistudyleader.identity.service.AuthServiceUnavailableException;
import com.studypot.aistudyleader.identity.service.AuthSessionMetadata;
import com.studypot.aistudyleader.identity.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.identity.service.AuthSessionService;
import com.studypot.aistudyleader.identity.service.AuthTokenCookiePort;
import com.studypot.aistudyleader.identity.service.AuthTokenResult;
import com.studypot.aistudyleader.identity.service.GoogleOAuthProfile;
import com.studypot.aistudyleader.identity.service.OAuthLoginRejectedException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Slf4j
@RequiredArgsConstructor
public class GoogleOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

	private final ObjectProvider<AuthSessionService> authSessionServiceProvider;
	private final AuthTokenCookiePort tokenCookiePort;
	private final AuthProperties properties;
	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	@Override
	public void onAuthenticationSuccess(
		HttpServletRequest request,
		HttpServletResponse response,
		Authentication authentication
	) throws IOException, ServletException {
		try {
			AuthTokenResult result = authSessionService()
				.loginWithGoogleProfile(profile(authentication), metadata(request));
			tokenCookiePort.addTokenCookies(response, result);
			invalidateSession(request);
			redirectStrategy.sendRedirect(request, response, properties.oauth2().frontendSuccessUri().toString());
		} catch (
			AuthServiceUnavailableException
			| AuthSessionRejectedException
			| OAuthLoginRejectedException exception
		) {
			log.info("Google OAuth2 login rejected: {}", exception.getMessage());
			invalidateSession(request);
			redirectStrategy.sendRedirect(request, response, properties.oauth2().frontendFailureUri().toString());
		}
	}

	private AuthSessionService authSessionService() {
		return authSessionServiceProvider.getIfAvailable(() -> {
			throw new AuthServiceUnavailableException("auth service is not configured.");
		});
	}

	private static GoogleOAuthProfile profile(Authentication authentication) {
		if (!(authentication instanceof OAuth2AuthenticationToken token) || !(token.getPrincipal() instanceof OAuth2User principal)) {
			throw new OAuthLoginRejectedException("Google OAuth2 principal is not available.");
		}
		Map<String, Object> attributes = principal.getAttributes();
		return new GoogleOAuthProfile(
			requiredString(attributes, "sub"),
			email(attributes),
			booleanAttribute(attributes, "email_verified"),
			optionalString(attributes, "name"),
			optionalString(attributes, "picture"),
			null,
			scope(token)
		);
	}

	private static EmailAddress email(Map<String, Object> attributes) {
		try {
			return EmailAddress.from(requiredString(attributes, "email"));
		} catch (IllegalArgumentException exception) {
			throw new OAuthLoginRejectedException("Google OAuth2 email is invalid.");
		}
	}

	private static String requiredString(Map<String, Object> attributes, String name) {
		String value = optionalString(attributes, name);
		if (value == null) {
			throw new OAuthLoginRejectedException("Google OAuth2 attribute is missing: " + name);
		}
		return value;
	}

	private static String optionalString(Map<String, Object> attributes, String name) {
		Object value = attributes.get(name);
		if (!(value instanceof String text) || text.isBlank()) {
			return null;
		}
		return text.strip();
	}

	private static boolean booleanAttribute(Map<String, Object> attributes, String name) {
		Object value = attributes.get(name);
		if (value instanceof Boolean bool) {
			return bool;
		}
		return value instanceof String text && Boolean.parseBoolean(text);
	}

	private static String scope(OAuth2AuthenticationToken token) {
		String scope = token.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.filter(authority -> authority.startsWith("SCOPE_"))
			.map(authority -> authority.substring("SCOPE_".length()))
			.sorted()
			.collect(Collectors.joining(" "));
		return scope.isBlank() ? null : scope;
	}

	private static AuthSessionMetadata metadata(HttpServletRequest request) {
		return new AuthSessionMetadata(request.getHeader("User-Agent"), clientIp(request));
	}

	private static String clientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null) {
			for (String value : forwardedFor.split(",")) {
				if (!value.isBlank()) {
					return value.strip();
				}
			}
		}
		return request.getRemoteAddr();
	}

	private static void invalidateSession(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
	}
}
