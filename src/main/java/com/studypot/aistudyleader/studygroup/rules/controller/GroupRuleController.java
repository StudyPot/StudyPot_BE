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
import com.studypot.aistudyleader.studygroup.rules.service.RecordRuleViolationCommand;
import com.studypot.aistudyleader.studygroup.rules.service.SaveGroupRuleCommand;
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

@RestController
@RequiredArgsConstructor
class GroupRuleController {

	private final ObjectProvider<GroupRuleService> groupRuleService;

	@PutMapping(ApiPaths.V1 + "/groups/{groupId}/rules/{ruleType}")
	GroupRuleResponse saveRule(
		Authentication authentication,
		@PathVariable UUID groupId,
		@PathVariable GroupRuleType ruleType,
		@Valid @RequestBody SaveRuleRequest request
	) {
		GroupRule rule = service().saveRule(request.toCommand(authenticatedUserId(authentication), groupId, ruleType));
		return GroupRuleResponse.from(rule);
	}

	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/rules")
	List<GroupRuleResponse> listRules(Authentication authentication, @PathVariable UUID groupId) {
		return service().listRules(new ListGroupRulesQuery(authenticatedUserId(authentication), groupId))
			.stream()
			.map(GroupRuleResponse::from)
			.toList();
	}

	@PatchMapping(ApiPaths.V1 + "/groups/{groupId}/rules/{ruleId}/deactivate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deactivateRule(Authentication authentication, @PathVariable UUID groupId, @PathVariable UUID ruleId) {
		service().deactivateRule(new DeactivateGroupRuleCommand(authenticatedUserId(authentication), groupId, ruleId));
	}

	@DeleteMapping(ApiPaths.V1 + "/groups/{groupId}/rules/{ruleId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteRule(Authentication authentication, @PathVariable UUID groupId, @PathVariable UUID ruleId) {
		service().deleteRule(new DeleteGroupRuleCommand(authenticatedUserId(authentication), groupId, ruleId));
	}

	@PostMapping(ApiPaths.V1 + "/groups/{groupId}/rule-violations")
	@ResponseStatus(HttpStatus.CREATED)
	RuleViolationResponse recordViolation(
		Authentication authentication,
		@PathVariable UUID groupId,
		@Valid @RequestBody RecordViolationRequest request
	) {
		RuleViolation violation = service().recordViolation(request.toCommand(authenticatedUserId(authentication), groupId));
		return RuleViolationResponse.from(violation);
	}

	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/rule-violations")
	List<RuleViolationResponse> listViolations(Authentication authentication, @PathVariable UUID groupId) {
		return service().listViolations(new ListGroupRulesQuery(authenticatedUserId(authentication), groupId))
			.stream()
			.map(RuleViolationResponse::from)
			.toList();
	}

	@PatchMapping(ApiPaths.V1 + "/groups/{groupId}/rule-violations/{violationId}/resolve")
	RuleViolationResponse resolveViolation(
		Authentication authentication,
		@PathVariable UUID groupId,
		@PathVariable UUID violationId,
		@Valid @RequestBody HandleViolationRequest request
	) {
		RuleViolation violation = service().resolveViolation(request.toCommand(authenticatedUserId(authentication), groupId, violationId));
		return RuleViolationResponse.from(violation);
	}

	@PatchMapping(ApiPaths.V1 + "/groups/{groupId}/rule-violations/{violationId}/waive")
	RuleViolationResponse waiveViolation(
		Authentication authentication,
		@PathVariable UUID groupId,
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

	private record SaveRuleRequest(
		@NotEmpty
		Map<String, Object> config,
		@Size(max = 2000)
		String description,
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

	private record RecordViolationRequest(
		@NotNull
		UUID ruleId,
		@NotNull
		UUID memberId,
		UUID taskCompletionId,
		@NotNull
		RuleViolationType violationType,
		Map<String, Object> details,
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

	private record HandleViolationRequest(
		@Size(max = 2000)
		String note
	) {

		HandleRuleViolationCommand toCommand(UUID authenticatedUserId, UUID groupId, UUID violationId) {
			return new HandleRuleViolationCommand(authenticatedUserId, groupId, violationId, note);
		}
	}

	private record GroupRuleResponse(
		UUID id,
		UUID groupId,
		UUID createdBy,
		GroupRuleType ruleType,
		Map<String, Object> config,
		String description,
		boolean active,
		Instant createdAt,
		Instant updatedAt,
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

	private record RuleViolationResponse(
		UUID id,
		UUID ruleId,
		UUID memberId,
		UUID taskCompletionId,
		RuleViolationType violationType,
		Map<String, Object> details,
		RuleViolationStatus status,
		Instant resolvedAt,
		String resolvedNote,
		Instant occurredAt,
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
