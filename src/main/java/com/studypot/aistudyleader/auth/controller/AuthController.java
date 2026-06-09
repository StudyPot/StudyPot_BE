package com.studypot.aistudyleader.auth.controller;

import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.auth.service.AuthSessionMetadata;
import com.studypot.aistudyleader.auth.service.AuthSessionService;
import com.studypot.aistudyleader.auth.service.AuthServiceUnavailableException;
import com.studypot.aistudyleader.auth.service.AuthTokenCookiePort;
import com.studypot.aistudyleader.auth.service.AuthTokenResult;
import com.studypot.aistudyleader.auth.service.RefreshTokenRejectedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
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

@Tag(name = "인증/사용자", description = "Google OAuth 최초 로그인, 회원 세션 가입, 토큰 갱신, 현재 사용자 정보를 관리하는 API입니다.")
@RestController
@RequiredArgsConstructor
class AuthController {

	private final ObjectProvider<AuthSessionService> authSessionService;
	private final ObjectProvider<AuthTokenCookiePort> tokenCookiePort;

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
			return AuthSessionResponse.from(result);
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
		authSessionService().logout(AuthenticatedPrincipal.userId(jwt), requireRefreshToken(servletRequest, null));
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
		authSessionService().logoutAll(AuthenticatedPrincipal.userId(jwt));
		tokenCookiePort.ifAvailable(port -> port.clearTokenCookies(servletResponse));
	}

	private AuthSessionService authSessionService() {
		return authSessionService.getIfAvailable(() -> {
			throw new AuthServiceUnavailableException("auth service is not configured.");
		});
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
		@Schema(description = "갱신된 세션에 연결된 현재 사용자 정보입니다.")
		AuthUserResponse user
	) {

		private static AuthSessionResponse from(AuthTokenResult result) {
			return new AuthSessionResponse(result.tokenType(), result.expiresIn(), AuthUserResponse.from(result.user()));
		}
	}
}
