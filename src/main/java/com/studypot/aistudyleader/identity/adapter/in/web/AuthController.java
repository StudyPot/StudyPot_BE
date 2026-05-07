package com.studypot.aistudyleader.identity.adapter.in.web;

import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.identity.application.AuthSessionMetadata;
import com.studypot.aistudyleader.identity.application.AuthSessionRejectedException;
import com.studypot.aistudyleader.identity.application.AuthSessionService;
import com.studypot.aistudyleader.identity.application.AuthServiceUnavailableException;
import com.studypot.aistudyleader.identity.application.AuthTokenResult;
import com.studypot.aistudyleader.identity.application.AuthenticatedUser;
import com.studypot.aistudyleader.identity.application.GoogleOAuthLoginCommand;
import com.studypot.aistudyleader.identity.application.InvalidAuthRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

	AuthController(ObjectProvider<AuthSessionService> authSessionService) {
		this.authSessionService = authSessionService;
	}

	@PostMapping(ApiPaths.V1 + "/auth/oauth/google")
	AuthTokenResponse loginWithGoogle(
		@Valid @RequestBody GoogleOAuthLoginRequest request,
		HttpServletRequest servletRequest
	) {
		return AuthTokenResponse.from(authSessionService().loginWithGoogle(request.toCommand(), metadata(servletRequest)));
	}

	@PostMapping(ApiPaths.V1 + "/auth/refresh")
	AuthTokenResponse refresh(
		@Valid @RequestBody RefreshTokenRequest request,
		HttpServletRequest servletRequest
	) {
		return AuthTokenResponse.from(authSessionService().refresh(request.refreshToken(), metadata(servletRequest)));
	}

	@PostMapping(ApiPaths.V1 + "/auth/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logout(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody LogoutRequest request) {
		authSessionService().logout(authenticatedUserId(jwt), request.refreshToken());
	}

	@PostMapping(ApiPaths.V1 + "/auth/logout-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logoutAll(@AuthenticationPrincipal Jwt jwt) {
		authSessionService().logoutAll(authenticatedUserId(jwt));
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
