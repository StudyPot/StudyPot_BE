package com.studypot.aistudyleader.auth.admin.controller;

import com.studypot.aistudyleader.auth.admin.AdminUserPlan;
import com.studypot.aistudyleader.auth.admin.AdminUserService;
import com.studypot.aistudyleader.auth.admin.AdminUserView;
import com.studypot.aistudyleader.auth.service.AuthServiceUnavailableException;
import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.global.api.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "운영", description = "사용자 요금제(FREE/PREMIUM)를 조회·변경하는 운영자 전용 API입니다.")
@RestController
@RequiredArgsConstructor
class AdminUserController {

	private final ObjectProvider<AdminUserService> adminUserService;

	@Operation(
		summary = "이메일로 사용자 조회(운영자)",
		description = "이메일로 활성 사용자를 찾아 식별자/닉네임/요금제를 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "사용자 정보 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "운영자 허용목록에 없어 접근할 수 없음"),
		@ApiResponse(responseCode = "404", description = "이메일에 해당하는 사용자가 없음"),
		@ApiResponse(responseCode = "503", description = "인증 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/admin/users")
	AdminUserResponse findByEmail(
		Authentication authentication,
		@Parameter(description = "조회할 사용자의 이메일입니다.", example = "member@studypot.dev")
		@RequestParam String email
	) {
		UUID requesterId = authenticatedUserId(authentication);
		return AdminUserResponse.from(service().findByEmail(requesterId, email));
	}

	@Operation(
		summary = "사용자 요금제 변경(운영자)",
		description = "대상 사용자의 요금제를 FREE 또는 PREMIUM으로 변경하고 갱신된 정보를 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "변경된 사용자 정보 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "운영자 허용목록에 없어 변경할 수 없음"),
		@ApiResponse(responseCode = "404", description = "대상 사용자가 없음"),
		@ApiResponse(responseCode = "422", description = "요금제 값이 비었거나 허용되지 않음"),
		@ApiResponse(responseCode = "503", description = "인증 서비스가 아직 구성되지 않음")
	})
	@PatchMapping(ApiPaths.V1 + "/admin/users/{userId}/plan")
	AdminUserResponse changePlan(
		Authentication authentication,
		@Parameter(description = "요금제를 변경할 대상 사용자 UUID입니다.", example = "018f6f55-6fb1-7d62-a711-25f7c6d16a28")
		@PathVariable UUID userId,
		@Valid @RequestBody ChangePlanRequest request
	) {
		UUID requesterId = authenticatedUserId(authentication);
		return AdminUserResponse.from(service().changePlan(requesterId, userId, request.plan()));
	}

	private AdminUserService service() {
		return adminUserService.getIfAvailable(() -> {
			throw new AuthServiceUnavailableException("admin user service is not configured.");
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

	@Schema(description = "사용자 요금제 변경 요청입니다.")
	record ChangePlanRequest(
		@Schema(description = "변경할 요금제입니다. FREE 또는 PREMIUM.", example = "PREMIUM")
		@NotNull AdminUserPlan plan
	) {
	}

	@Schema(description = "운영자 사용자 관리 응답(식별자/이메일/닉네임/요금제)입니다.")
	record AdminUserResponse(
		@Schema(description = "사용자 UUID입니다.", example = "018f6f55-6fb1-7d62-a711-25f7c6d16a28")
		UUID id,
		@Schema(description = "사용자 이메일입니다.", example = "member@studypot.dev")
		String email,
		@Schema(description = "사용자 닉네임입니다.", example = "현우")
		String nickname,
		@Schema(description = "사용자 요금제입니다. FREE 또는 PREMIUM.", example = "PREMIUM")
		String plan
	) {

		static AdminUserResponse from(AdminUserView view) {
			return new AdminUserResponse(view.id(), view.email(), view.nickname(), view.plan());
		}
	}
}
