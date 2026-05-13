package com.studypot.aistudyleader.llm.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsage;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.service.ListGroupLlmUsageQuery;
import com.studypot.aistudyleader.llm.service.LlmUsageService;
import com.studypot.aistudyleader.llm.service.LlmUsageServiceUnavailableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "운영", description = "스터디 그룹 운영자가 확인하는 LLM 사용 기록 API입니다.")
@RestController
@RequiredArgsConstructor
class LlmUsageController {

	private final ObjectProvider<LlmUsageService> llmUsageService;

	@Operation(
		summary = "그룹 LLM 사용 기록 조회",
		description = "인증된 그룹 운영자가 해당 그룹의 LLM 호출 목적, 모델, 토큰, 상태 기록을 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "LLM 사용 기록 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "그룹 운영자가 아니어서 사용 기록을 조회할 수 없음"),
		@ApiResponse(responseCode = "404", description = "스터디 그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "LLM 사용 기록 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/llm-usage")
	List<LlmUsageResponse> listGroupUsage(
		Authentication authentication,
		@Parameter(description = "LLM 사용 기록을 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		return service().listGroupUsage(new ListGroupLlmUsageQuery(authenticatedUserId(authentication), groupId))
			.stream()
			.map(LlmUsageResponse::from)
			.toList();
	}

	private LlmUsageService service() {
		return llmUsageService.getIfAvailable(() -> {
			throw new LlmUsageServiceUnavailableException("LLM usage service is not configured.");
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

	@Schema(description = "LLM 사용 기록 응답입니다.")
	private record LlmUsageResponse(
		@Schema(description = "LLM 사용 기록 UUID입니다.", example = "018f6f55-900d-7b14-bd27-48ec1d752b8a")
		UUID id,
		@Schema(description = "LLM 호출 목적입니다.", example = "TEAM_LEAD_CHAT")
		LlmUsagePurpose purpose,
		@Schema(description = "LLM 제공자입니다.", example = "OPENAI")
		LlmProvider provider,
		@Schema(description = "호출한 모델명입니다.", example = "gpt-4.1-mini")
		String model,
		@Schema(description = "입력 토큰 수입니다.", example = "120")
		int inputTokens,
		@Schema(description = "출력 토큰 수입니다.", example = "45")
		int outputTokens,
		@Schema(description = "LLM 호출 상태입니다.", example = "SUCCESS")
		LlmUsageStatus status
	) {

		private static LlmUsageResponse from(LlmUsage usage) {
			return new LlmUsageResponse(
				usage.id(),
				usage.purpose(),
				usage.provider(),
				usage.model(),
				usage.inputTokens(),
				usage.outputTokens(),
				usage.status()
			);
		}
	}
}
