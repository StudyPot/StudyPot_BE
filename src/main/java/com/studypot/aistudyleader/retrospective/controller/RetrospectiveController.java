package com.studypot.aistudyleader.retrospective.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.retrospective.domain.Retrospective;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveStatus;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveTriggerType;
import com.studypot.aistudyleader.retrospective.service.GetMyRetrospectiveQuery;
import com.studypot.aistudyleader.retrospective.service.ListMyRetrospectivesQuery;
import com.studypot.aistudyleader.retrospective.service.RequestRetrospectiveCommand;
import com.studypot.aistudyleader.retrospective.service.RetrospectiveService;
import com.studypot.aistudyleader.retrospective.service.RetrospectiveServiceUnavailableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회고", description = "주차 진행 상태를 바탕으로 내 AI 팀장 회고 레코드의 생성과 조회를 다루는 API입니다.")
@RestController
@RequiredArgsConstructor
class RetrospectiveController {

	private final ObjectProvider<RetrospectiveService> retrospectiveService;

	@Operation(
		summary = "내 주차 회고 요청",
		description = "인증된 활성 멤버의 주차 진행 상태에서 회고 레코드를 생성하거나 기존 회고 상태를 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "202", description = "회고 요청이 접수되고 상태 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "해당 주차가 속한 그룹의 활성 멤버가 아니어서 요청할 수 없음"),
		@ApiResponse(responseCode = "404", description = "주차, 주차 진행 상태, 또는 회고 대상을 찾을 수 없음"),
		@ApiResponse(responseCode = "409", description = "회고 생성 상태를 저장할 수 없음"),
		@ApiResponse(responseCode = "503", description = "회고 서비스가 아직 구성되지 않음")
	})
	@PostMapping(ApiPaths.V1 + "/weeks/{weekId}/retrospectives/me")
	@ResponseStatus(HttpStatus.ACCEPTED)
	RetrospectiveResponse requestRetrospective(
		Authentication authentication,
		@Parameter(description = "회고를 요청할 커리큘럼 주차 UUID입니다.", required = true)
		@PathVariable UUID weekId
	) {
		Retrospective retrospective = service().requestMyRetrospective(new RequestRetrospectiveCommand(
			authenticatedUserId(authentication),
			weekId,
			RetrospectiveTriggerType.MANUAL
		));
		return RetrospectiveResponse.from(retrospective);
	}

	@Operation(
		summary = "내 주차 회고 조회",
		description = "인증된 활성 멤버가 특정 주차에 대해 이미 생성된 본인 회고 상태와 저장된 AI 출력 JSON을 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "내 회고 상태 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "해당 주차가 속한 그룹의 활성 멤버가 아니어서 조회할 수 없음"),
		@ApiResponse(responseCode = "404", description = "주차, 주차 진행 상태, 또는 회고를 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "회고 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/weeks/{weekId}/retrospectives/me")
	RetrospectiveResponse getMyRetrospective(
		Authentication authentication,
		@Parameter(description = "회고를 조회할 커리큘럼 주차 UUID입니다.", required = true)
		@PathVariable UUID weekId
	) {
		Retrospective retrospective = service().getMyRetrospective(new GetMyRetrospectiveQuery(authenticatedUserId(authentication), weekId));
		return RetrospectiveResponse.from(retrospective);
	}

	@Operation(
		summary = "내 그룹 회고 전체 조회",
		description = "인증된 활성 멤버가 그룹 내 자신의 모든 주차 회고를 최신 주차 순으로 조회합니다. (리뷰 화면에서 이번 주/지난 주 회고 확인)"
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "내 회고 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹의 활성 멤버가 아니어서 조회할 수 없음"),
		@ApiResponse(responseCode = "503", description = "회고 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/retrospectives/me")
	java.util.List<RetrospectiveResponse> listMyRetrospectives(
		Authentication authentication,
		@Parameter(description = "회고를 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		return service().listMyRetrospectives(new ListMyRetrospectivesQuery(authenticatedUserId(authentication), groupId))
			.stream()
			.map(RetrospectiveResponse::from)
			.toList();
	}

	private RetrospectiveService service() {
		return retrospectiveService.getIfAvailable(() -> {
			throw new RetrospectiveServiceUnavailableException("retrospective service is not configured.");
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

	@Schema(description = "내 주차 회고 상태 응답입니다.")
	private record RetrospectiveResponse(
		@Schema(description = "회고 UUID입니다.", example = "018f6f55-900d-7b14-bd27-48ec1d752b8a")
		UUID id,
		@Schema(description = "회고 처리 상태입니다.", example = "PENDING")
		RetrospectiveStatus status,
		@Schema(description = "AI 팀장 피드백 JSON입니다. SPT-38 생성 직후에는 빈 객체입니다.", example = "{\"summary\":\"이번 주 학습 흐름이 좋습니다.\"}")
		Map<String, Object> aiFeedback,
		@Schema(description = "다음 주 조정 제안 JSON입니다. SPT-38 생성 직후에는 빈 객체입니다.", example = "{\"focus\":\"JPA 심화\"}")
		Map<String, Object> nextWeekAdjustment
	) {

		private static RetrospectiveResponse from(Retrospective retrospective) {
			return new RetrospectiveResponse(
				retrospective.id(),
				retrospective.status(),
				retrospective.aiFeedback(),
				retrospective.nextWeekAdjustment()
			);
		}
	}
}
