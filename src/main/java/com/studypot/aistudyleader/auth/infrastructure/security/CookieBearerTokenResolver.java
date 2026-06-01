package com.studypot.aistudyleader.auth.infrastructure.security;

import com.studypot.aistudyleader.global.api.ApiPaths;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

@RequiredArgsConstructor
class CookieBearerTokenResolver implements BearerTokenResolver {

	private final DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
	private final AuthTokenCookieIssuer tokenCookieIssuer;

	@Override
	public String resolve(HttpServletRequest request) {
		String headerToken = delegate.resolve(request);
		if (headerToken != null) {
			return headerToken;
		}
		if (isPublicCookieRefreshEndpoint(request)) {
			return null;
		}
		return tokenCookieIssuer.accessToken(request).orElse(null);
	}

	private static boolean isPublicCookieRefreshEndpoint(HttpServletRequest request) {
		return HttpMethod.POST.matches(request.getMethod())
			&& (ApiPaths.V1 + "/auth/refresh").equals(request.getRequestURI());
	}
}
