package com.studypot.aistudyleader.auth.controller;

import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.auth.service.AuthSessionMetadata;
import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.auth.service.AuthSessionService;
import com.studypot.aistudyleader.auth.service.AuthServiceUnavailableException;
import com.studypot.aistudyleader.auth.service.AuthTokenCookiePort;
import com.studypot.aistudyleader.auth.service.AuthTokenResult;
import com.studypot.aistudyleader.auth.service.AuthenticatedUser;
import com.studypot.aistudyleader.auth.service.RefreshTokenRejectedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class AuthController {

	private final ObjectProvider<AuthSessionService> authSessionService;
	private final ObjectProvider<AuthTokenCookiePort> tokenCookiePort;

	@PostMapping(ApiPaths.V1 + "/auth/refresh")
	AuthSessionResponse refresh(
		HttpServletRequest servletRequest,
		HttpServletResponse servletResponse
	) {
		AuthSessionService service = authSessionService();
		String refreshToken = requireRefreshTokenCookie(servletRequest);
		AuthTokenResult result = service.refresh(refreshToken, metadata(servletRequest));
		tokenCookiePort.ifAvailable(port -> port.addTokenCookies(servletResponse, result));
		return AuthSessionResponse.from(result);
	}

	@PostMapping(ApiPaths.V1 + "/auth/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logout(
		@AuthenticationPrincipal Jwt jwt,
		HttpServletRequest servletRequest,
		HttpServletResponse servletResponse
	) {
		authSessionService().logout(authenticatedUserId(jwt), requireRefreshTokenCookie(servletRequest));
		tokenCookiePort.ifAvailable(port -> port.clearTokenCookies(servletResponse));
	}

	@PostMapping(ApiPaths.V1 + "/auth/logout-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logoutAll(@AuthenticationPrincipal Jwt jwt, HttpServletResponse servletResponse) {
		authSessionService().logoutAll(authenticatedUserId(jwt));
		tokenCookiePort.ifAvailable(port -> port.clearTokenCookies(servletResponse));
	}

	@GetMapping(ApiPaths.V1 + "/users/me")
	UserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
		return UserResponse.from(authSessionService().currentUser(authenticatedUserId(jwt)));
	}

	private AuthSessionService authSessionService() {
		return authSessionService.getIfAvailable(() -> {
			throw new AuthServiceUnavailableException("auth service is not configured.");
		});
	}

	private static UUID authenticatedUserId(Jwt jwt) {
		if (jwt == null || jwt.getSubject() == null) {
			throw new AuthSessionRejectedException("authenticated user is required.");
		}
		try {
			return UUID.fromString(jwt.getSubject());
		} catch (IllegalArgumentException exception) {
			throw new AuthSessionRejectedException("authenticated user is invalid.");
		}
	}

	private static AuthSessionMetadata metadata(HttpServletRequest request) {
		return new AuthSessionMetadata(request.getHeader("User-Agent"), request.getRemoteAddr());
	}

	private String requireRefreshTokenCookie(HttpServletRequest servletRequest) {
		return refreshTokenCookie(servletRequest)
			.orElseThrow(() -> new RefreshTokenRejectedException("refresh token is required."));
	}

	private Optional<String> refreshTokenCookie(HttpServletRequest servletRequest) {
		AuthTokenCookiePort port = tokenCookiePort.getIfAvailable();
		return port == null ? Optional.empty() : port.refreshToken(servletRequest);
	}

	private record AuthSessionResponse(String tokenType, long expiresIn, UserResponse user) {

		private static AuthSessionResponse from(AuthTokenResult result) {
			return new AuthSessionResponse(result.tokenType(), result.expiresIn(), UserResponse.from(result.user()));
		}
	}

	private record UserResponse(UUID id, String email, String nickname) {

		private static UserResponse from(AuthenticatedUser user) {
			return new UserResponse(user.id(), user.email(), user.nickname());
		}
	}
}
