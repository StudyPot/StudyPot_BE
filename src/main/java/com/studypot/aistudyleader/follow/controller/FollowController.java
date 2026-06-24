package com.studypot.aistudyleader.follow.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.follow.domain.FollowUserView;
import com.studypot.aistudyleader.follow.service.FollowService;
import com.studypot.aistudyleader.follow.service.FollowServiceUnavailableException;
import com.studypot.aistudyleader.follow.service.FollowToggleResult;
import com.studypot.aistudyleader.global.api.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "팔로우", description = "사용자가 다른 사용자를 팔로우/언팔로우하고 팔로잉·팔로워 목록을 조회하는 API입니다.")
@RestController
@RequiredArgsConstructor
class FollowController {

	private final ObjectProvider<FollowService> followService;

	@Operation(summary = "팔로우 토글", description = "대상 사용자에 대한 팔로우 상태를 토글하고 변경된 상태를 반환합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "팔로우 토글 결과 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "404", description = "대상 사용자를 찾을 수 없음"),
		@ApiResponse(responseCode = "422", description = "자기 자신은 팔로우할 수 없음"),
		@ApiResponse(responseCode = "503", description = "팔로우 서비스가 아직 구성되지 않음")
	})
	@PostMapping(ApiPaths.V1 + "/users/{userId}/follow")
	FollowToggleResponse toggleFollow(
		Authentication authentication,
		@Parameter(description = "팔로우를 토글할 대상 사용자 UUID입니다.", required = true)
		@PathVariable UUID userId
	) {
		FollowToggleResult result = service().toggleFollow(authenticatedUserId(authentication), userId);
		return new FollowToggleResponse(result.userId(), result.following());
	}

	@Operation(summary = "내 팔로잉 목록", description = "인증된 사용자가 팔로우하는 사용자 목록을 최신순으로 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "팔로잉 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "503", description = "팔로우 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/users/me/following")
	List<FollowUserResponse> listFollowing(Authentication authentication) {
		return service().listFollowing(authenticatedUserId(authentication)).stream()
			.map(FollowUserResponse::from)
			.toList();
	}

	@Operation(summary = "내 팔로워 목록", description = "인증된 사용자를 팔로우하는 사용자 목록을 최신순으로 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "팔로워 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "503", description = "팔로우 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/users/me/followers")
	List<FollowUserResponse> listFollowers(Authentication authentication) {
		return service().listFollowers(authenticatedUserId(authentication)).stream()
			.map(FollowUserResponse::from)
			.toList();
	}

	private FollowService service() {
		return followService.getIfAvailable(() -> {
			throw new FollowServiceUnavailableException("follow service is not configured.");
		});
	}

	private static UUID authenticatedUserId(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AuthSessionRejectedException("authenticated user is required.");
		}
		String subject = authenticatedSubject(authentication);
		if (subject == null || subject.isBlank()) {
			throw new AuthSessionRejectedException("authenticated user is required.");
		}
		try {
			return UUID.fromString(subject);
		} catch (IllegalArgumentException exception) {
			throw new AuthSessionRejectedException("authenticated user is invalid.");
		}
	}

	private static String authenticatedSubject(Authentication authentication) {
		Object principal = authentication.getPrincipal();
		if (principal instanceof Jwt jwt) {
			return jwt.getSubject();
		}
		return authentication.getName();
	}

	@Schema(description = "팔로우 토글 결과입니다.")
	private record FollowToggleResponse(
		@Schema(description = "대상 사용자 UUID입니다.", example = "018f6f55-6fb1-7d62-a711-25f7c6d16a28")
		UUID userId,
		@Schema(description = "토글 후 팔로우 여부입니다.", example = "true")
		boolean following
	) {
	}

	@Schema(description = "팔로잉/팔로워 목록의 사용자 항목입니다.")
	private record FollowUserResponse(
		@Schema(description = "사용자 UUID입니다.", example = "018f6f55-6fb1-7d62-a711-25f7c6d16a28")
		UUID userId,
		@Schema(description = "사용자 닉네임입니다.", example = "현우")
		String nickname,
		@Schema(description = "사용자 이메일입니다.", example = "member@studypot.dev")
		String email,
		@Schema(description = "자기소개입니다. 없으면 null.", nullable = true)
		String bio,
		@Schema(description = "맞팔 여부입니다.", example = "true")
		boolean mutual,
		@Schema(description = "팔로우한 시각입니다.", example = "2026-06-22T10:00:00Z")
		Instant followedAt
	) {

		private static FollowUserResponse from(FollowUserView view) {
			return new FollowUserResponse(view.userId(), view.nickname(), view.email(), view.bio(), view.mutual(), view.followedAt());
		}
	}
}
