package com.studypot.aistudyleader.auth.controller;

import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.global.ratelimit.AiConversationQuotaView;
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
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DeferredCsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
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
		summary = "CSRF 토큰 발급",
		description = "다른 site의 브라우저 프론트엔드가 cookie-backed unsafe 요청에 사용할 `X-XSRF-TOKEN` 값을 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "CSRF 토큰 쿠키와 프론트엔드가 읽을 수 있는 토큰 값이 발급됨")
	})
	@GetMapping(ApiPaths.V1 + "/auth/csrf")
	CsrfTokenResponse csrf(
		@RequestAttribute(name = "org.springframework.security.web.csrf.DeferredCsrfToken") DeferredCsrfToken deferredCsrfToken,
		HttpServletResponse servletResponse
	) {
		CsrfToken csrfToken = deferredCsrfToken.get();
		String token = csrfToken.getToken();
		servletResponse.setHeader(csrfToken.getHeaderName(), token);
		return new CsrfTokenResponse("XSRF-TOKEN", csrfToken.getHeaderName(), token);
	}

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
		@RequestBody(required = false) RefreshTokenRequest requestBody,
		HttpServletRequest servletRequest,
		HttpServletResponse servletResponse
	) {
		AuthSessionService service = authSessionService();
		try {
			String refreshToken = requireRefreshToken(servletRequest, requestBody);
			AuthTokenResult result = service.refresh(refreshToken, metadata(servletRequest));
			tokenCookiePort.ifAvailable(port -> port.addTokenCookies(servletResponse, result));
			return AuthSessionResponse.from(result, service.currentPlan(result.user().id()));
		} catch (RefreshTokenRejectedException exception) {
			tokenCookiePort.ifAvailable(port -> port.clearTokenCookies(servletResponse));
			throw exception;
		}
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
		authSessionService().logout(authenticatedUserId(jwt), requireRefreshToken(servletRequest, null));
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
		AuthSessionService service = authSessionService();
		return UserResponse.from(service.currentUser(userId), service.currentPlan(userId));
	}

	@Operation(
		summary = "내 프로필 수정",
		description = "인증된 사용자의 닉네임/자기소개를 수정하고 갱신된 프로필을 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "수정된 사용자 정보 반환"),
		@ApiResponse(responseCode = "400", description = "닉네임이 비었거나 길이 제한을 위반함"),
		@ApiResponse(responseCode = "401", description = "인증 정보가 없거나 사용자 식별자를 해석할 수 없음"),
		@ApiResponse(responseCode = "503", description = "인증 서비스가 아직 구성되지 않음")
	})
	@org.springframework.web.bind.annotation.PatchMapping(ApiPaths.V1 + "/users/me")
	UserResponse updateCurrentUser(
		@AuthenticationPrincipal Jwt jwt,
		@jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody UpdateUserRequest request
	) {
		UUID userId = authenticatedUserId(jwt);
		rateLimitGuard.ifAvailable(guard -> guard.checkUsersMe(userId));
		AuthSessionService service = authSessionService();
		AuthenticatedUser updated = service.updateProfile(userId, request.nickname(), request.bio());
		return UserResponse.from(updated, service.currentPlan(userId));
	}

	@Operation(
		summary = "AI 팀장 대화 일일 잔여 횟수 조회",
		description = "인증된 사용자의 요금제 기준 AI 팀장 채팅 일일 한도와 현재 사용량/잔여 횟수, 리셋까지 남은 시간을 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "일일 한도/사용량/잔여 횟수 반환"),
		@ApiResponse(responseCode = "401", description = "인증 정보가 없거나 사용자 식별자를 해석할 수 없음"),
		@ApiResponse(responseCode = "503", description = "인증 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/users/me/ai-quota")
	AiQuotaResponse aiQuota(@AuthenticationPrincipal Jwt jwt) {
		UUID userId = authenticatedUserId(jwt);
		String plan = authSessionService().currentPlan(userId);
		RateLimitGuard guard = rateLimitGuard.getIfAvailable();
		if (guard == null) {
			// 한도 미적용(레이트리밋 비구성) — 사실상 무제한으로 본다.
			return new AiQuotaResponse(plan, -1, 0, -1, 0);
		}
		AiConversationQuotaView quota = guard.aiConversationQuota(userId, plan);
		return new AiQuotaResponse(plan, quota.dailyLimit(), quota.used(), quota.remaining(), quota.resetSeconds());
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

	private String requireRefreshToken(HttpServletRequest servletRequest, RefreshTokenRequest requestBody) {
		return refreshTokenCookie(servletRequest)
			.or(() -> refreshTokenBody(requestBody))
			.orElseThrow(RefreshTokenRejectedException::required);
	}

	private Optional<String> refreshTokenCookie(HttpServletRequest servletRequest) {
		AuthTokenCookiePort port = tokenCookiePort.getIfAvailable();
		return port == null ? Optional.empty() : port.refreshToken(servletRequest);
	}

	private static Optional<String> refreshTokenBody(RefreshTokenRequest requestBody) {
		return Optional.ofNullable(requestBody)
			.map(RefreshTokenRequest::refreshToken)
			.filter(value -> !value.isBlank());
	}

	@Schema(description = "토큰 갱신 요청입니다. HttpOnly refresh-token 쿠키가 있으면 쿠키 값이 우선합니다.")
	private record RefreshTokenRequest(
		@Schema(description = "쿠키가 없는 호환 클라이언트가 제출하는 refresh token입니다.")
		String refreshToken
	) {
	}

	@Schema(description = "브라우저가 cookie-backed unsafe 요청에 사용할 CSRF 토큰 응답입니다.")
	private record CsrfTokenResponse(
		@Schema(description = "백엔드 도메인에 설정되는 CSRF 쿠키 이름입니다.", example = "XSRF-TOKEN")
		String cookieName,
		@Schema(description = "프론트엔드가 unsafe 요청에 포함해야 하는 헤더 이름입니다.", example = "X-XSRF-TOKEN")
		String headerName,
		@Schema(description = "프론트엔드가 헤더 값으로 보내야 하는 CSRF 토큰입니다.")
		String token
	) {
	}

	@Schema(description = "토큰 갱신 후 클라이언트가 확인할 수 있는 세션 요약 응답입니다.")
	private record AuthSessionResponse(
		@Schema(description = "발급된 access token의 토큰 타입입니다.", example = "Bearer")
		String tokenType,
		@Schema(description = "새 access token의 남은 유효 시간(초)입니다.", example = "3600")
		long expiresIn,
		@Schema(description = "새 access token. 쿠키를 못 쓰는 클라이언트(모바일/크로스도메인)가 Authorization 헤더에 사용합니다.")
		String accessToken,
		@Schema(description = "새 refresh token. 쿠키를 못 쓰는 클라이언트가 다음 갱신 요청 바디에 사용합니다.")
		String refreshToken,
		@Schema(description = "갱신된 세션에 연결된 현재 사용자 정보입니다.")
		UserResponse user
	) {

		private static AuthSessionResponse from(AuthTokenResult result, String plan) {
			return new AuthSessionResponse(
				result.tokenType(),
				result.expiresIn(),
				result.accessToken(),
				result.refreshToken(),
				UserResponse.from(result.user(), plan)
			);
		}
	}

	@Schema(description = "AI 팀장 대화 일일 잔여 횟수 응답입니다.")
	private record AiQuotaResponse(
		@Schema(description = "사용자 요금제입니다. FREE 또는 PREMIUM.", example = "FREE")
		String plan,
		@Schema(description = "일일 한도입니다. 한도 미적용 시 -1.", example = "15")
		long dailyLimit,
		@Schema(description = "현재 윈도우에서 사용한 횟수입니다.", example = "3")
		long used,
		@Schema(description = "남은 횟수입니다. 한도 미적용 시 -1.", example = "12")
		long remaining,
		@Schema(description = "일일 한도가 리셋되기까지 남은 초입니다.", example = "43200")
		long resetSeconds
	) {
	}

	@Schema(description = "인증된 사용자의 기본 프로필 응답입니다.")
	private record UserResponse(
		@Schema(description = "사용자 UUID입니다.", example = "018f6f55-6fb1-7d62-a711-25f7c6d16a28")
		UUID id,
		@Schema(description = "Google OAuth 계정에서 확인한 이메일 주소입니다.", example = "member@studypot.dev")
		String email,
		@Schema(description = "서비스에서 표시할 사용자 닉네임입니다.", example = "현우")
		String nickname,
		@Schema(description = "자기소개입니다. 없으면 null.", example = "백엔드 공부 중입니다.", nullable = true)
		String bio,
		@Schema(description = "사용자 요금제입니다. FREE 또는 PREMIUM.", example = "FREE")
		String plan
	) {

		private static UserResponse from(AuthenticatedUser user, String plan) {
			return new UserResponse(user.id(), user.email(), user.nickname(), user.bio(), plan);
		}
	}

	@Schema(description = "내 프로필(닉네임/자기소개) 수정 요청입니다.")
	private record UpdateUserRequest(
		@Schema(description = "변경할 닉네임입니다. 1~80자.", example = "현우")
		@jakarta.validation.constraints.NotBlank
		@jakarta.validation.constraints.Size(max = 80)
		String nickname,
		@Schema(description = "변경할 자기소개입니다. 0~500자.", example = "백엔드 공부 중입니다.", nullable = true)
		@jakarta.validation.constraints.Size(max = 500)
		String bio
	) {
	}
}
