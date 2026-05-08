package com.studypot.aistudyleader.identity.infrastructure.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

@Slf4j
@RequiredArgsConstructor
public class GoogleOAuth2LoginFailureHandler implements AuthenticationFailureHandler {

	private final AuthProperties properties;
	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	@Override
	public void onAuthenticationFailure(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException exception
	) throws IOException, ServletException {
		log.warn("Google OAuth2 login failed");
		log.debug("Google OAuth2 failure details", exception);
		invalidateSession(request);
		redirectStrategy.sendRedirect(request, response, properties.oauth2().frontendFailureUri().toString());
	}

	private static void invalidateSession(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
	}
}
