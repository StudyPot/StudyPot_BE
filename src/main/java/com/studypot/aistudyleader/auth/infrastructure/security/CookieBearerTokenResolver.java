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
		// EventSource(SSE)는 Authorization 헤더를 실을 수 없어, 스트림 엔드포인트에 한해 access_token 쿼리파라미터를 허용한다.
		String streamToken = streamQueryToken(request);
		if (streamToken != null) {
			return streamToken;
		}
		if (isPublicCookieRefreshEndpoint(request)) {
			return null;
		}
		return tokenCookieIssuer.accessToken(request).orElse(null);
	}

	private static String streamQueryToken(HttpServletRequest request) {
		String uri = request.getRequestURI();
		if (uri != null && uri.endsWith("/notifications/stream")) {
			String token = request.getParameter("access_token");
			if (token != null && !token.isBlank()) {
				return token;
			}
		}
		return null;
	}

	private static boolean isPublicCookieRefreshEndpoint(HttpServletRequest request) {
		return HttpMethod.POST.matches(request.getMethod())
			&& (ApiPaths.V1 + "/auth/refresh").equals(request.getRequestURI());
	}
}
