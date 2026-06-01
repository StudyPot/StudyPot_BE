package com.studypot.aistudyleader.ai.controller;

import com.studypot.aistudyleader.ai.domain.AiConversation;
import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import com.studypot.aistudyleader.ai.domain.AiConversationType;
import com.studypot.aistudyleader.ai.service.AiConversationService;
import com.studypot.aistudyleader.ai.service.AiConversationServiceUnavailableException;
import com.studypot.aistudyleader.ai.service.ListAiConversationMessagesQuery;
import com.studypot.aistudyleader.ai.service.OpenAiConversationCommand;
import com.studypot.aistudyleader.ai.service.SendAiConversationMessageCommand;
import com.studypot.aistudyleader.ai.service.SubscribeAiConversationStreamQuery;
import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.global.api.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "AI 팀장", description = "그룹 멤버의 AI 팀장 대화 세션을 다루는 API입니다.")
@RestController
@RequiredArgsConstructor
class AiConversationController {

	private final ObjectProvider<AiConversationService> aiConversationService;
	private final ObjectProvider<AiConversationStreamService> aiConversationStreamService;

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

	@Operation(
		summary = "AI 팀장 대화 메시지 전송",
		description = "인증된 대화 멤버의 사용자 메시지를 저장하고, 구성된 LLM provider로 AI 팀장 응답을 생성해 저장합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "AI 대화 메시지와 AI 응답 저장"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 대화의 멤버가 아니거나 메시지를 저장할 수 없는 멤버 상태"),
		@ApiResponse(responseCode = "404", description = "대상 AI 대화를 찾을 수 없음"),
		@ApiResponse(responseCode = "409", description = "닫힌 대화라 메시지를 저장할 수 없음"),
		@ApiResponse(responseCode = "422", description = "요청 형식이 올바르지 않음"),
		@ApiResponse(responseCode = "503", description = "AI 대화 서비스가 구성되지 않았거나 AI 응답 생성에 실패함")
	})
	@PostMapping(ApiPaths.V1 + "/ai-conversations/{conversationId}/messages")
	@ResponseStatus(HttpStatus.CREATED)
	AiConversationMessageResponse sendMessage(
		Authentication authentication,
		@Parameter(description = "메시지를 저장할 AI 대화 세션 UUID입니다.", required = true)
		@PathVariable UUID conversationId,
		@Valid @RequestBody CreateMessageRequest request
	) {
		AiConversationMessage message = service().sendMessage(request.toCommand(authenticatedUserId(authentication), conversationId));
		return AiConversationMessageResponse.from(message);
	}

	@Operation(
		summary = "AI 팀장 대화 메시지 조회",
		description = "인증된 대화 멤버가 재연결 복구나 초기 렌더링을 위해 AI 팀장 대화 메시지를 커서 기반으로 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "AI 대화 메시지 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 대화의 활성 멤버가 아님"),
		@ApiResponse(responseCode = "404", description = "대상 AI 대화를 찾을 수 없음"),
		@ApiResponse(responseCode = "422", description = "커서 또는 페이지 크기가 올바르지 않음"),
		@ApiResponse(responseCode = "503", description = "AI 대화 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/ai-conversations/{conversationId}/messages")
	CursorPageResponse<AiConversationMessageResponse> listMessages(
		Authentication authentication,
		@Parameter(description = "메시지를 조회할 AI 대화 세션 UUID입니다.", required = true)
		@PathVariable UUID conversationId,
		@Parameter(description = "다음 페이지 조회용 커서입니다.")
		@RequestParam(required = false) String cursor,
		@Parameter(description = "조회할 메시지 수입니다. 1부터 100까지 허용됩니다.")
		@RequestParam(defaultValue = "20") int pageSize
	) {
		CursorPageResponse<AiConversationMessage> page = service().listMessages(
			new ListAiConversationMessagesQuery(authenticatedUserId(authentication), conversationId, cursor, pageSize)
		);
		return new CursorPageResponse<>(
			page.items().stream().map(AiConversationMessageResponse::from).toList(),
			page.pageInfo()
		);
	}

	@Operation(
		summary = "AI 팀장 대화 SSE 구독",
		description = "인증된 대화 멤버가 AI 팀장 대화의 메시지 저장과 assistant 생성 이벤트를 SSE로 구독합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "SSE 연결 시작"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 대화의 활성 멤버가 아님"),
		@ApiResponse(responseCode = "404", description = "대상 AI 대화를 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "AI 대화 스트림 서비스가 아직 구성되지 않음")
	})
	@GetMapping(path = ApiPaths.V1 + "/ai-conversations/{conversationId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	SseEmitter subscribeConversationStream(
		Authentication authentication,
		@Parameter(description = "SSE로 구독할 AI 대화 세션 UUID입니다.", required = true)
		@PathVariable UUID conversationId
	) {
		UUID authenticatedUserId = authenticatedUserId(authentication);
		service().validateConversationStreamAccess(new SubscribeAiConversationStreamQuery(authenticatedUserId, conversationId));
		return streamService().subscribe(conversationId);
	}

	private AiConversationService service() {
		return aiConversationService.getIfAvailable(() -> {
			throw new AiConversationServiceUnavailableException("AI conversation service is not configured.");
		});
	}

	private AiConversationStreamService streamService() {
		return aiConversationStreamService.getIfAvailable(() -> {
			throw new AiConversationServiceUnavailableException("AI conversation stream service is not configured.");
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

	@Schema(description = "AI 팀장 대화 메시지 저장 요청입니다.")
	private record CreateMessageRequest(
		@NotBlank
		@Schema(description = "사용자 메시지 본문입니다.", example = "이번 주 과제 양을 조금 줄이고 싶어요.")
		String content
	) {

		private SendAiConversationMessageCommand toCommand(UUID authenticatedUserId, UUID conversationId) {
			return new SendAiConversationMessageCommand(authenticatedUserId, conversationId, content);
		}
	}

}
