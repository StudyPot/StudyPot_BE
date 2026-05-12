package com.studypot.aistudyleader.studygroup.rules.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.studygroup.rules.domain.GroupRule;
import com.studypot.aistudyleader.studygroup.rules.domain.GroupRuleType;
import com.studypot.aistudyleader.studygroup.rules.domain.RuleViolation;
import com.studypot.aistudyleader.studygroup.rules.domain.RuleViolationStatus;
import com.studypot.aistudyleader.studygroup.rules.domain.RuleViolationType;
import com.studypot.aistudyleader.studygroup.rules.service.DeactivateGroupRuleCommand;
import com.studypot.aistudyleader.studygroup.rules.service.DeleteGroupRuleCommand;
import com.studypot.aistudyleader.studygroup.rules.service.GroupRuleService;
import com.studypot.aistudyleader.studygroup.rules.service.GroupRuleServiceUnavailableException;
import com.studypot.aistudyleader.studygroup.rules.service.HandleRuleViolationCommand;
import com.studypot.aistudyleader.studygroup.rules.service.ListGroupRulesQuery;
import com.studypot.aistudyleader.studygroup.rules.service.ListRuleViolationsQuery;
import com.studypot.aistudyleader.studygroup.rules.service.RecordRuleViolationCommand;
import com.studypot.aistudyleader.studygroup.rules.service.SaveGroupRuleCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "그룹 규칙/위반", description = "스터디 그룹의 운영 규칙과 규칙 위반 기록을 저장, 조회, 처리하는 API입니다.")
@RestController
@RequiredArgsConstructor
class GroupRuleController {

	private final ObjectProvider<GroupRuleService> groupRuleService;

	@Operation(
		summary = "그룹 규칙 저장",
		description = "규칙 유형별 설정 JSON과 설명, 활성 여부를 저장합니다. 같은 규칙 유형은 최신 설정으로 갱신됩니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "그룹 규칙이 저장되고 최신 규칙 반환"),
		@ApiResponse(responseCode = "400", description = "규칙 설정이 비어 있거나 요청 형식이 올바르지 않음"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "그룹 규칙을 관리할 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "그룹 규칙 서비스가 아직 구성되지 않음")
	})
	@PutMapping(ApiPaths.V1 + "/groups/{groupId}/rules/{ruleType}")
	GroupRuleResponse saveRule(
		Authentication authentication,
		@Parameter(description = "규칙을 저장할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Parameter(description = "저장할 그룹 규칙 유형입니다.", required = true)
		@PathVariable GroupRuleType ruleType,
		@Valid @RequestBody SaveRuleRequest request
	) {
		GroupRule rule = service().saveRule(request.toCommand(authenticatedUserId(authentication), groupId, ruleType));
		return GroupRuleResponse.from(rule);
	}

	@Operation(
		summary = "그룹 규칙 목록 조회",
		description = "스터디 그룹에 설정된 활성/비활성 규칙 목록을 조회합니다. 삭제된 규칙은 응답의 deletedAt으로 구분됩니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "그룹 규칙 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹의 멤버가 아니어서 조회할 수 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "그룹 규칙 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/rules")
	List<GroupRuleResponse> listRules(
		Authentication authentication,
		@Parameter(description = "규칙 목록을 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		return service().listRules(new ListGroupRulesQuery(authenticatedUserId(authentication), groupId))
			.stream()
			.map(GroupRuleResponse::from)
			.toList();
	}

	@Operation(
		summary = "그룹 규칙 비활성화",
		description = "규칙을 삭제하지 않고 비활성 상태로 전환합니다. 비활성 규칙은 향후 위반 판정 대상에서 제외됩니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "그룹 규칙이 비활성화됨"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "그룹 규칙을 관리할 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹 또는 규칙을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "그룹 규칙 서비스가 아직 구성되지 않음")
	})
	@PatchMapping(ApiPaths.V1 + "/groups/{groupId}/rules/{ruleId}/deactivate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deactivateRule(
		Authentication authentication,
		@Parameter(description = "규칙이 속한 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Parameter(description = "비활성화할 그룹 규칙 UUID입니다.", required = true)
		@PathVariable UUID ruleId
	) {
		service().deactivateRule(new DeactivateGroupRuleCommand(authenticatedUserId(authentication), groupId, ruleId));
	}

	@Operation(
		summary = "그룹 규칙 삭제",
		description = "그룹 규칙을 soft delete 처리해 일반 운영 흐름에서 제외합니다. 기존 위반 기록은 감사 목적으로 유지됩니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "그룹 규칙이 삭제 처리됨"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "그룹 규칙을 관리할 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹 또는 규칙을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "그룹 규칙 서비스가 아직 구성되지 않음")
	})
	@DeleteMapping(ApiPaths.V1 + "/groups/{groupId}/rules/{ruleId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteRule(
		Authentication authentication,
		@Parameter(description = "규칙이 속한 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Parameter(description = "삭제 처리할 그룹 규칙 UUID입니다.", required = true)
		@PathVariable UUID ruleId
	) {
		service().deleteRule(new DeleteGroupRuleCommand(authenticatedUserId(authentication), groupId, ruleId));
	}

	@Operation(
		summary = "규칙 위반 기록",
		description = "특정 멤버가 그룹 규칙을 위반한 사실을 기록합니다. 위반 상세는 유연한 JSON 맵으로 저장합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "규칙 위반 기록이 생성되고 상세 반환"),
		@ApiResponse(responseCode = "400", description = "필수 식별자 또는 위반 유형이 누락됨"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "위반 기록을 생성할 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹, 규칙 또는 멤버를 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "그룹 규칙 서비스가 아직 구성되지 않음")
	})
	@PostMapping(ApiPaths.V1 + "/groups/{groupId}/rule-violations")
	@ResponseStatus(HttpStatus.CREATED)
	RuleViolationResponse recordViolation(
		Authentication authentication,
		@Parameter(description = "규칙 위반을 기록할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Valid @RequestBody RecordViolationRequest request
	) {
		RuleViolation violation = service().recordViolation(request.toCommand(authenticatedUserId(authentication), groupId));
		return RuleViolationResponse.from(violation);
	}

	@Operation(
		summary = "규칙 위반 목록 조회",
		description = "스터디 그룹에 기록된 규칙 위반 목록과 처리 상태를 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "규칙 위반 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹의 멤버가 아니어서 조회할 수 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "그룹 규칙 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/rule-violations")
	List<RuleViolationResponse> listViolations(
		Authentication authentication,
		@Parameter(description = "규칙 위반 목록을 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		return service().listViolations(new ListRuleViolationsQuery(authenticatedUserId(authentication), groupId))
			.stream()
			.map(RuleViolationResponse::from)
			.toList();
	}

	@Operation(
		summary = "규칙 위반 해결 처리",
		description = "열려 있는 규칙 위반을 해결 상태로 전환하고 처리 메모와 처리 시각을 기록합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "규칙 위반이 해결 처리되고 최신 상태 반환"),
		@ApiResponse(responseCode = "400", description = "처리 메모 길이 등 요청 검증에 실패함"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "위반 기록을 처리할 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹 또는 위반 기록을 찾을 수 없음"),
		@ApiResponse(responseCode = "409", description = "이미 처리된 위반 기록이라 상태를 변경할 수 없음"),
		@ApiResponse(responseCode = "503", description = "그룹 규칙 서비스가 아직 구성되지 않음")
	})
	@PatchMapping(ApiPaths.V1 + "/groups/{groupId}/rule-violations/{violationId}/resolve")
	RuleViolationResponse resolveViolation(
		Authentication authentication,
		@Parameter(description = "위반 기록이 속한 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Parameter(description = "해결 처리할 규칙 위반 UUID입니다.", required = true)
		@PathVariable UUID violationId,
		@Valid @RequestBody HandleViolationRequest request
	) {
		RuleViolation violation = service().resolveViolation(request.toCommand(authenticatedUserId(authentication), groupId, violationId));
		return RuleViolationResponse.from(violation);
	}

	@Operation(
		summary = "규칙 위반 면제 처리",
		description = "열려 있는 규칙 위반을 면제 상태로 전환하고 면제 사유 또는 운영 메모를 기록합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "규칙 위반이 면제 처리되고 최신 상태 반환"),
		@ApiResponse(responseCode = "400", description = "처리 메모 길이 등 요청 검증에 실패함"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "위반 기록을 처리할 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹 또는 위반 기록을 찾을 수 없음"),
		@ApiResponse(responseCode = "409", description = "이미 처리된 위반 기록이라 상태를 변경할 수 없음"),
		@ApiResponse(responseCode = "503", description = "그룹 규칙 서비스가 아직 구성되지 않음")
	})
	@PatchMapping(ApiPaths.V1 + "/groups/{groupId}/rule-violations/{violationId}/waive")
	RuleViolationResponse waiveViolation(
		Authentication authentication,
		@Parameter(description = "위반 기록이 속한 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Parameter(description = "면제 처리할 규칙 위반 UUID입니다.", required = true)
		@PathVariable UUID violationId,
		@Valid @RequestBody HandleViolationRequest request
	) {
		RuleViolation violation = service().waiveViolation(request.toCommand(authenticatedUserId(authentication), groupId, violationId));
		return RuleViolationResponse.from(violation);
	}

	private GroupRuleService service() {
		return groupRuleService.getIfAvailable(() -> {
			throw new GroupRuleServiceUnavailableException("group rule service is not configured.");
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

	@Schema(description = "그룹 규칙 저장 요청입니다.")
	private record SaveRuleRequest(
		@Schema(description = "규칙 유형별 세부 설정 JSON입니다.", example = "{\"maxAbsences\":2,\"period\":\"WEEKLY\"}")
		@NotEmpty
		Map<String, Object> config,
		@Schema(description = "그룹원이 이해할 수 있는 규칙 설명입니다.", example = "주 2회 이상 불참하면 운영자가 확인합니다.")
		@Size(max = 2000)
		String description,
		@Schema(description = "규칙 활성 여부입니다. null이면 true로 처리합니다.", example = "true")
		Boolean active
	) {

		SaveGroupRuleCommand toCommand(UUID authenticatedUserId, UUID groupId, GroupRuleType ruleType) {
			return new SaveGroupRuleCommand(
				authenticatedUserId,
				groupId,
				ruleType,
				config,
				description,
				active == null || active
			);
		}
	}

	@Schema(description = "규칙 위반 기록 요청입니다.")
	private record RecordViolationRequest(
		@Schema(description = "위반된 그룹 규칙 UUID입니다.", example = "018f6f55-96c8-76e5-bf08-a89fd546a92f")
		@NotNull
		UUID ruleId,
		@Schema(description = "위반 대상 그룹 멤버 UUID입니다.", example = "018f6f55-75e9-78d2-9f5c-598945b93400")
		@NotNull
		UUID memberId,
		@Schema(description = "위반이 특정 과제 완료 기록과 연결될 때 사용하는 task_completion UUID입니다.", example = "018f6f55-8f6c-7334-a781-84152e57e4f4")
		UUID taskCompletionId,
		@Schema(description = "위반 유형입니다.", example = "MISSING_REQUIRED_TASK")
		@NotNull
		RuleViolationType violationType,
		@Schema(description = "위반 판단에 사용된 상세 정보 JSON입니다.", example = "{\"missedTaskTitle\":\"연관관계 매핑 실습\"}")
		Map<String, Object> details,
		@Schema(description = "위반이 실제 발생한 시각입니다. null이면 서버 기준 생성 시각을 사용합니다.", example = "2026-05-24T12:00:00Z")
		Instant occurredAt
	) {

		RecordRuleViolationCommand toCommand(UUID authenticatedUserId, UUID groupId) {
			return new RecordRuleViolationCommand(
				authenticatedUserId,
				groupId,
				ruleId,
				memberId,
				taskCompletionId,
				violationType,
				details == null ? Map.of() : details,
				occurredAt
			);
		}
	}

	@Schema(description = "규칙 위반 해결/면제 처리 요청입니다.")
	private record HandleViolationRequest(
		@Schema(description = "운영자가 남기는 해결 또는 면제 메모입니다.", example = "사전 공유된 일정 충돌로 면제 처리합니다.")
		@Size(max = 2000)
		String note
	) {

		HandleRuleViolationCommand toCommand(UUID authenticatedUserId, UUID groupId, UUID violationId) {
			return new HandleRuleViolationCommand(authenticatedUserId, groupId, violationId, note);
		}
	}

	@Schema(description = "그룹 규칙 조회/저장 응답입니다.")
	private record GroupRuleResponse(
		@Schema(description = "그룹 규칙 UUID입니다.", example = "018f6f55-96c8-76e5-bf08-a89fd546a92f")
		UUID id,
		@Schema(description = "규칙이 속한 스터디 그룹 UUID입니다.", example = "018f6f55-6fb1-7d62-a711-25f7c6d16a28")
		UUID groupId,
		@Schema(description = "규칙을 생성하거나 마지막으로 저장한 사용자 UUID입니다.", example = "018f6f55-6f42-7e11-b479-120c5f2e9d42")
		UUID createdBy,
		@Schema(description = "그룹 규칙 유형입니다.", example = "ATTENDANCE")
		GroupRuleType ruleType,
		@Schema(description = "규칙 유형별 세부 설정 JSON입니다.", example = "{\"maxAbsences\":2,\"period\":\"WEEKLY\"}")
		Map<String, Object> config,
		@Schema(description = "그룹원이 이해할 수 있는 규칙 설명입니다.", example = "주 2회 이상 불참하면 운영자가 확인합니다.")
		String description,
		@Schema(description = "규칙 활성 여부입니다.", example = "true")
		boolean active,
		@Schema(description = "규칙 생성 시각입니다.", example = "2026-05-18T09:00:00Z")
		Instant createdAt,
		@Schema(description = "규칙 마지막 수정 시각입니다.", example = "2026-05-19T09:00:00Z")
		Instant updatedAt,
		@Schema(description = "규칙 삭제 처리 시각입니다. 삭제되지 않았다면 null입니다.", example = "2026-05-20T09:00:00Z")
		Instant deletedAt
	) {

		private static GroupRuleResponse from(GroupRule rule) {
			return new GroupRuleResponse(
				rule.id(),
				rule.groupId(),
				rule.createdBy(),
				rule.ruleType(),
				rule.config(),
				rule.description(),
				rule.active(),
				rule.createdAt(),
				rule.updatedAt(),
				rule.deletedAt().orElse(null)
			);
		}
	}

	@Schema(description = "규칙 위반 조회/처리 응답입니다.")
	private record RuleViolationResponse(
		@Schema(description = "규칙 위반 UUID입니다.", example = "018f6f55-9810-712d-96c6-67d7d549e847")
		UUID id,
		@Schema(description = "위반된 그룹 규칙 UUID입니다.", example = "018f6f55-96c8-76e5-bf08-a89fd546a92f")
		UUID ruleId,
		@Schema(description = "위반 대상 그룹 멤버 UUID입니다.", example = "018f6f55-75e9-78d2-9f5c-598945b93400")
		UUID memberId,
		@Schema(description = "연결된 과제 완료 기록 UUID입니다. 과제와 무관한 위반이면 null입니다.", example = "018f6f55-8f6c-7334-a781-84152e57e4f4")
		UUID taskCompletionId,
		@Schema(description = "규칙 위반 유형입니다.", example = "MISSING_REQUIRED_TASK")
		RuleViolationType violationType,
		@Schema(description = "위반 판단에 사용된 상세 정보 JSON입니다.", example = "{\"missedTaskTitle\":\"연관관계 매핑 실습\"}")
		Map<String, Object> details,
		@Schema(description = "위반 처리 상태입니다.", example = "OPEN")
		RuleViolationStatus status,
		@Schema(description = "해결 또는 면제 처리 시각입니다. 아직 열려 있으면 null입니다.", example = "2026-05-24T12:30:00Z")
		Instant resolvedAt,
		@Schema(description = "운영자가 남긴 해결 또는 면제 메모입니다.", example = "사전 공유된 일정 충돌로 면제 처리합니다.")
		String resolvedNote,
		@Schema(description = "위반이 실제 발생한 시각입니다.", example = "2026-05-24T12:00:00Z")
		Instant occurredAt,
		@Schema(description = "위반 기록 생성 시각입니다.", example = "2026-05-24T12:05:00Z")
		Instant createdAt
	) {

		private static RuleViolationResponse from(RuleViolation violation) {
			return new RuleViolationResponse(
				violation.id(),
				violation.ruleId(),
				violation.memberId(),
				violation.taskCompletionId().orElse(null),
				violation.violationType(),
				violation.details(),
				violation.status(),
				violation.resolvedAt().orElse(null),
				violation.resolvedNote().orElse(null),
				violation.occurredAt(),
				violation.createdAt()
			);
		}
	}
}
