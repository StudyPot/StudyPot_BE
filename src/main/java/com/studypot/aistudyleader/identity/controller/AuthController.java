package com.studypot.aistudyleader.identity.controller;

import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.identity.service.AuthSessionMetadata;
import com.studypot.aistudyleader.identity.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.identity.service.AuthSessionService;
import com.studypot.aistudyleader.identity.service.AuthServiceUnavailableException;
import com.studypot.aistudyleader.identity.service.AuthTokenResult;
import com.studypot.aistudyleader.identity.service.AuthenticatedUser;
import com.studypot.aistudyleader.identity.infrastructure.security.AuthTokenCookieIssuer;
import com.studypot.aistudyleader.identity.service.GoogleOAuthLoginCommand;
import com.studypot.aistudyleader.identity.service.InvalidAuthRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AuthController {

	private final ObjectProvider<AuthSessionService> authSessionService;
	private final ObjectProvider<AuthTokenCookieIssuer> tokenCookieIssuer;

	AuthController(
		ObjectProvider<AuthSessionService> authSessionService,
		ObjectProvider<AuthTokenCookieIssuer> tokenCookieIssuer
	) {
		this.authSessionService = authSessionService;
		this.tokenCookieIssuer = tokenCookieIssuer;
	}

	@PostMapping(ApiPaths.V1 + "/auth/oauth/google")
	AuthTokenResponse loginWithGoogle(
		@Valid @RequestBody GoogleOAuthLoginRequest request,
		HttpServletRequest servletRequest,
		HttpServletResponse servletResponse
	) {
		AuthTokenResult result = authSessionService().loginWithGoogle(request.toCommand(), metadata(servletRequest));
		tokenCookieIssuer.ifAvailable(issuer -> issuer.addTokenCookies(servletResponse, result));
		return AuthTokenResponse.from(result);
	}

	@PostMapping(ApiPaths.V1 + "/auth/refresh")
	AuthTokenResponse refresh(
		@Valid @RequestBody(required = false) RefreshTokenRequest request,
		HttpServletRequest servletRequest,
		HttpServletResponse servletResponse
	) {
		AuthTokenResult result = authSessionService().refresh(refreshTokenFrom(request, servletRequest), metadata(servletRequest));
		tokenCookieIssuer.ifAvailable(issuer -> issuer.addTokenCookies(servletResponse, result));
		return AuthTokenResponse.from(result);
	}

	@PostMapping(ApiPaths.V1 + "/auth/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logout(
		@AuthenticationPrincipal Jwt jwt,
		@Valid @RequestBody(required = false) LogoutRequest request,
		HttpServletRequest servletRequest,
		HttpServletResponse servletResponse
	) {
		authSessionService().logout(authenticatedUserId(jwt), refreshTokenFrom(request, servletRequest));
		tokenCookieIssuer.ifAvailable(issuer -> issuer.clearTokenCookies(servletResponse));
	}

	@PostMapping(ApiPaths.V1 + "/auth/logout-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logoutAll(@AuthenticationPrincipal Jwt jwt, HttpServletResponse servletResponse) {
		authSessionService().logoutAll(authenticatedUserId(jwt));
		tokenCookieIssuer.ifAvailable(issuer -> issuer.clearTokenCookies(servletResponse));
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

	private String refreshTokenFrom(RefreshTokenRequest request, HttpServletRequest servletRequest) {
		String refreshToken = request == null ? null : request.refreshToken();
		if (refreshToken == null || refreshToken.isBlank()) {
			refreshToken = refreshTokenCookie(servletRequest).orElse(null);
		}
		return requireRefreshToken(refreshToken);
	}

	private String refreshTokenFrom(LogoutRequest request, HttpServletRequest servletRequest) {
		String refreshToken = request == null ? null : request.refreshToken();
		if (refreshToken == null || refreshToken.isBlank()) {
			refreshToken = refreshTokenCookie(servletRequest).orElse(null);
		}
		return requireRefreshToken(refreshToken);
	}

	private Optional<String> refreshTokenCookie(HttpServletRequest servletRequest) {
		AuthTokenCookieIssuer issuer = tokenCookieIssuer.getIfAvailable();
		return issuer == null ? Optional.empty() : issuer.refreshToken(servletRequest);
	}

	private static String requireRefreshToken(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new InvalidAuthRequestException("refreshToken", "refreshToken must not be blank");
		}
		return refreshToken.strip();
	}

	private record GoogleOAuthLoginRequest(
		@NotBlank String authorizationCode,
		@NotBlank String redirectUri,
		String codeVerifier
	) {

		private GoogleOAuthLoginCommand toCommand() {
			try {
				return new GoogleOAuthLoginCommand(authorizationCode, redirectUri, codeVerifier);
			} catch (IllegalArgumentException exception) {
				throw new InvalidAuthRequestException(fieldFrom(exception), exception.getMessage());
			}
		}

		private static String fieldFrom(IllegalArgumentException exception) {
			String message = exception.getMessage();
			if (message != null && message.startsWith("redirectUri ")) {
				return "redirectUri";
			}
			if (message != null && message.startsWith("authorizationCode ")) {
				return "authorizationCode";
			}
			return "request";
		}
	}

	private record RefreshTokenRequest(@NotBlank String refreshToken) {
	}

	private record LogoutRequest(@NotBlank String refreshToken) {
	}

	private record AuthTokenResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long expiresIn,
		UserResponse user
	) {

		private static AuthTokenResponse from(AuthTokenResult result) {
			return new AuthTokenResponse(
				result.accessToken(),
				result.refreshToken(),
				result.tokenType(),
				result.expiresIn(),
				UserResponse.from(result.user())
			);
		}
	}

	private record UserResponse(UUID id, String email, String nickname) {

		private static UserResponse from(AuthenticatedUser user) {
			return new UserResponse(user.id(), user.email(), user.nickname());
		}
	}
}
