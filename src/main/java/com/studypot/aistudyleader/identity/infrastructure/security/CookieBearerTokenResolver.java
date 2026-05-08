package com.studypot.aistudyleader.identity.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

class CookieBearerTokenResolver implements BearerTokenResolver {

	private final DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
	private final AuthTokenCookieIssuer tokenCookieIssuer;

	CookieBearerTokenResolver(AuthTokenCookieIssuer tokenCookieIssuer) {
		this.tokenCookieIssuer = tokenCookieIssuer;
	}

	@Override
	public String resolve(HttpServletRequest request) {
		String headerToken = delegate.resolve(request);
		if (headerToken != null) {
			return headerToken;
		}
		return tokenCookieIssuer.accessToken(request).orElse(null);
	}
}
