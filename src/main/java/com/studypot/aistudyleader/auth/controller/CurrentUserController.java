package com.studypot.aistudyleader.auth.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionService;
import com.studypot.aistudyleader.auth.service.AuthServiceUnavailableException;
import com.studypot.aistudyleader.auth.service.UpdateCurrentUserProfileCommand;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.global.ratelimit.RateLimitGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "현재 사용자", description = "인증된 사용자의 기본 정보와 프로필을 관리하는 API입니다.")
@RestController
@RequiredArgsConstructor
class CurrentUserController {

	private final ObjectProvider<AuthSessionService> authSessionService;
	private final ObjectProvider<RateLimitGuard> rateLimitGuard;

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
	AuthUserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
		UUID userId = AuthenticatedPrincipal.userId(jwt);
		rateLimitGuard.ifAvailable(guard -> guard.checkUsersMe(userId));
		return AuthUserResponse.from(authSessionService().currentUser(userId));
	}

	@Operation(
		summary = "현재 사용자 프로필 수정",
		description = "인증된 사용자의 닉네임, 프로필 이미지, 자기소개, 관심 주제, 숙련도를 수정합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "프로필 수정 결과 반환"),
		@ApiResponse(responseCode = "401", description = "인증 정보가 없거나 사용자 식별자를 해석할 수 없음"),
		@ApiResponse(responseCode = "422", description = "닉네임 또는 프로필 입력값이 유효하지 않음"),
		@ApiResponse(responseCode = "503", description = "인증 서비스가 아직 구성되지 않음")
	})
	@PatchMapping(ApiPaths.V1 + "/users/me")
	AuthUserResponse updateCurrentUser(
		@AuthenticationPrincipal Jwt jwt,
		@Valid @RequestBody UpdateCurrentUserRequest request
	) {
		return AuthUserResponse.from(authSessionService().updateCurrentUserProfile(
			AuthenticatedPrincipal.userId(jwt),
			request.toCommand()
		));
	}

	private AuthSessionService authSessionService() {
		return authSessionService.getIfAvailable(() -> {
			throw new AuthServiceUnavailableException("auth service is not configured.");
		});
	}

	@Schema(description = "현재 사용자 프로필 수정 요청입니다.")
	private record UpdateCurrentUserRequest(
		@Schema(description = "서비스 표시 닉네임입니다.", example = "현우")
		@NotBlank
		@Size(max = 80)
		String nickname,
		@Schema(description = "사용자 프로필 이미지 URL입니다.", example = "https://cdn.studypot.dev/profiles/member.png")
		@Size(max = 2048)
		String profileImage,
		@Schema(description = "사용자 자기소개입니다.", example = "백엔드와 Vue를 함께 공부합니다.")
		@Size(max = 1000)
		String bio,
		@Schema(description = "관심 학습 주제 목록입니다.", example = "[\"Spring Boot\", \"JPA\"]")
		@Size(max = 20)
		List<@NotBlank @Size(max = 80) String> preferredTopics,
		@Schema(description = "사용자 숙련도입니다.", example = "intermediate")
		@Pattern(regexp = "beginner|intermediate|advanced")
		String skillLevel
	) {

		private UpdateCurrentUserProfileCommand toCommand() {
			return new UpdateCurrentUserProfileCommand(nickname, profileImage, bio, preferredTopics, skillLevel);
		}
	}
}
