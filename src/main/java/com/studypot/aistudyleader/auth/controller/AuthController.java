package com.studypot.aistudyleader.auth.controller;

import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.global.ratelimit.RateLimitGuard;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "인증/사용자", description = "Google OAuth 기반 세션 토큰과 현재 사용자 정보를 관리하는 API입니다.")
@RestController
@RequiredArgsConstructor
class AuthController {

	private final ObjectProvider<AuthSessionService> authSessionService;
	private final ObjectProvider<AuthTokenCookiePort> tokenCookiePort;
	private final ObjectProvider<RateLimitGuard> rateLimitGuard;

	@Operation(
		summary = "인증 토큰 갱신",
		description = "브라우저의 `studypot_refresh_token` HttpOnly 쿠키를 검증해 새 access/refresh token 쿠키를 발급하고 현재 사용자 요약을 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "토큰 쿠키가 회전되고 현재 사용자 정보가 반환됨"),
		@ApiResponse(responseCode = "401", description = "refresh token 쿠키가 없거나 만료/폐기되어 갱신할 수 없음"),
		@ApiResponse(responseCode = "503", description = "인증 서비스가 아직 구성되지 않음")
	})
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

	@Operation(
		summary = "현재 세션 로그아웃",
		description = "인증된 사용자의 현재 refresh token 쿠키를 폐기하고 access/refresh token 쿠키를 브라우저에서 제거합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "현재 세션 토큰이 폐기되고 쿠키가 삭제됨"),
		@ApiResponse(responseCode = "401", description = "인증 정보 또는 refresh token 쿠키가 유효하지 않음"),
		@ApiResponse(responseCode = "503", description = "인증 서비스가 아직 구성되지 않음")
	})
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

	@Operation(
		summary = "모든 세션 로그아웃",
		description = "인증된 사용자의 모든 활성 refresh token을 폐기하고 현재 브라우저의 토큰 쿠키도 제거합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "사용자의 모든 refresh token이 폐기되고 쿠키가 삭제됨"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "503", description = "인증 서비스가 아직 구성되지 않음")
	})
	@PostMapping(ApiPaths.V1 + "/auth/logout-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logoutAll(@AuthenticationPrincipal Jwt jwt, HttpServletResponse servletResponse) {
		authSessionService().logoutAll(authenticatedUserId(jwt));
		tokenCookiePort.ifAvailable(port -> port.clearTokenCookies(servletResponse));
	}

	@Operation(
		summary = "현재 사용자 조회",
		description = "bearer token 또는 `studypot_access_token` 쿠키로 인증된 사용자의 식별자, 이메일, 닉네임을 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "현재 사용자 정보 반환"),
		@ApiResponse(responseCode = "401", description = "인증 정보가 없거나 사용자 식별자를 해석할 수 없음"),
		@ApiResponse(responseCode = "503", description = "인증 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/users/me")
	UserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
		UUID userId = authenticatedUserId(jwt);
		rateLimitGuard.ifAvailable(guard -> guard.checkUsersMe(userId));
		return UserResponse.from(authSessionService().currentUser(userId));
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

	@Schema(description = "토큰 갱신 후 클라이언트가 확인할 수 있는 세션 요약 응답입니다.")
	private record AuthSessionResponse(
		@Schema(description = "발급된 access token의 토큰 타입입니다.", example = "Bearer")
		String tokenType,
		@Schema(description = "새 access token의 남은 유효 시간(초)입니다.", example = "3600")
		long expiresIn,
		@Schema(description = "갱신된 세션에 연결된 현재 사용자 정보입니다.")
		UserResponse user
	) {

		private static AuthSessionResponse from(AuthTokenResult result) {
			return new AuthSessionResponse(result.tokenType(), result.expiresIn(), UserResponse.from(result.user()));
		}
	}

	@Schema(description = "인증된 사용자의 기본 프로필 응답입니다.")
	private record UserResponse(
		@Schema(description = "사용자 UUID입니다.", example = "018f6f55-6fb1-7d62-a711-25f7c6d16a28")
		UUID id,
		@Schema(description = "Google OAuth 계정에서 확인한 이메일 주소입니다.", example = "member@studypot.dev")
		String email,
		@Schema(description = "서비스에서 표시할 사용자 닉네임입니다.", example = "현우")
		String nickname
	) {

		private static UserResponse from(AuthenticatedUser user) {
			return new UserResponse(user.id(), user.email(), user.nickname());
		}
	}
}
