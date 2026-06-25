package com.studypot.aistudyleader.llm.admin;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.service.LlmUsageServiceUnavailableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "운영", description = "전체 그룹의 LLM 사용 기록을 확인하는 운영자 전용 API입니다.")
@RestController
@RequiredArgsConstructor
class AdminLlmUsageController {

	private final ObjectProvider<AdminLlmUsageService> adminLlmUsageService;

	@Operation(summary = "운영자 여부 확인", description = "현재 로그인한 사용자가 운영자 허용목록에 포함되는지 반환합니다.")
	@GetMapping(ApiPaths.V1 + "/admin/me")
	AdminIdentityResponse me(Authentication authentication) {
		AdminLlmUsageService.AdminIdentity identity = service().identify(authenticatedUserId(authentication));
		return new AdminIdentityResponse(identity.email(), identity.admin());
	}

	@Operation(
		summary = "전체 LLM 사용 기록 조회(운영자)",
		description = "모든 그룹을 가로질러 그룹/사용자/목적/상태/기간 필터로 LLM 사용 기록과 집계 요약을 조회합니다."
	)
	@GetMapping(ApiPaths.V1 + "/admin/llm-usage")
	AdminLlmUsagePage listUsage(
		Authentication authentication,
		@RequestParam(required = false) UUID groupId,
		@RequestParam(required = false) UUID userId,
		@RequestParam(required = false) LlmUsagePurpose purpose,
		@RequestParam(required = false) LlmUsageStatus status,
		@RequestParam(required = false) Instant from,
		@RequestParam(required = false) Instant to,
		@RequestParam(required = false, defaultValue = "0") int limit
	) {
		UUID requesterId = authenticatedUserId(authentication);
		AdminLlmUsageFilter filter = new AdminLlmUsageFilter(groupId, userId, purpose, status, from, to, limit);
		AdminLlmUsageService service = service();
		AdminLlmUsageSummary summary = service.summarize(requesterId, filter);
		List<AdminLlmUsageRow> items = service.listUsage(requesterId, filter);
		return new AdminLlmUsagePage(summary, items);
	}

	private AdminLlmUsageService service() {
		return adminLlmUsageService.getIfAvailable(() -> {
			throw new LlmUsageServiceUnavailableException("admin LLM usage service is not configured.");
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

	record AdminIdentityResponse(String email, boolean admin) {
	}

	record AdminLlmUsagePage(AdminLlmUsageSummary summary, List<AdminLlmUsageRow> items) {
	}
}
