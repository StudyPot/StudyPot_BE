package com.studypot.aistudyleader.ai.controller;

import com.studypot.aistudyleader.ai.domain.AiConversation;
import com.studypot.aistudyleader.ai.domain.AiConversationStatus;
import com.studypot.aistudyleader.ai.domain.AiConversationType;
import com.studypot.aistudyleader.ai.service.AiConversationService;
import com.studypot.aistudyleader.ai.service.AiConversationServiceUnavailableException;
import com.studypot.aistudyleader.ai.service.OpenAiConversationCommand;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 팀장", description = "그룹 멤버의 AI 팀장 대화 세션을 다루는 API입니다.")
@RestController
@RequiredArgsConstructor
class AiConversationController {

	private final ObjectProvider<AiConversationService> aiConversationService;

	@Operation(
		summary = "AI 팀장 대화 세션 열기",
		description = "인증된 활성 멤버가 그룹 안에서 일반 AI 팀장 대화 또는 회고 연결 대화 세션을 생성합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "AI 대화 세션 생성"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹의 활성 멤버가 아니거나 연결 리소스 접근 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "그룹, 주차, 또는 회고를 찾을 수 없음"),
		@ApiResponse(responseCode = "422", description = "요청 형식이 올바르지 않음"),
		@ApiResponse(responseCode = "503", description = "AI 대화 서비스가 아직 구성되지 않음")
	})
	@PostMapping(ApiPaths.V1 + "/groups/{groupId}/ai-conversations")
	@ResponseStatus(HttpStatus.CREATED)
	AiConversationResponse openConversation(
		Authentication authentication,
		@Parameter(description = "AI 대화 세션을 열 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Valid @RequestBody OpenConversationRequest request
	) {
		AiConversation conversation = service().openConversation(request.toCommand(authenticatedUserId(authentication), groupId));
		return AiConversationResponse.from(conversation);
	}

	private AiConversationService service() {
		return aiConversationService.getIfAvailable(() -> {
			throw new AiConversationServiceUnavailableException("AI conversation service is not configured.");
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

	@Schema(description = "AI 팀장 대화 세션 생성 요청입니다.")
	private record OpenConversationRequest(
		@NotNull
		@Schema(description = "대화 유형입니다.", example = "TEAM_LEAD_CHAT")
		AiConversationType conversationType,
		@Schema(description = "대화가 연결될 커리큘럼 주차 UUID입니다.", example = "018f6f55-900d-7b14-bd27-48ec1d752b8a")
		UUID weekId,
		@Schema(description = "회고 연결 대화일 때 연결할 회고 UUID입니다.", example = "018f6f55-900d-7b14-bd27-48ec1d752b8b")
		UUID retrospectiveId
	) {

		private OpenAiConversationCommand toCommand(UUID authenticatedUserId, UUID groupId) {
			return new OpenAiConversationCommand(authenticatedUserId, groupId, conversationType, weekId, retrospectiveId);
		}
	}

	@Schema(description = "AI 팀장 대화 세션 응답입니다.")
	private record AiConversationResponse(
		@Schema(description = "AI 대화 세션 UUID입니다.", example = "018f6f55-900d-7b14-bd27-48ec1d752b8a")
		UUID id,
		@Schema(description = "대화 유형입니다.", example = "TEAM_LEAD_CHAT")
		AiConversationType conversationType,
		@Schema(description = "대화 세션 상태입니다.", example = "OPEN")
		AiConversationStatus status,
		@Schema(description = "현재까지의 대화 요약입니다.", example = "")
		String summary
	) {

		private static AiConversationResponse from(AiConversation conversation) {
			return new AiConversationResponse(
				conversation.id(),
				conversation.conversationType(),
				conversation.status(),
				conversation.summary()
			);
		}
	}
}
