package com.studypot.aistudyleader.studygroup.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import com.studypot.aistudyleader.studygroup.service.CreateStudyGroupCommand;
import com.studypot.aistudyleader.studygroup.service.JoinStudyGroupCommand;
import com.studypot.aistudyleader.studygroup.service.ListStudyGroupsQuery;
import com.studypot.aistudyleader.studygroup.service.StudyGroupCreationResult;
import com.studypot.aistudyleader.studygroup.service.StudyGroupJoinResult;
import com.studypot.aistudyleader.studygroup.service.StudyGroupService;
import com.studypot.aistudyleader.studygroup.service.StudyGroupServiceUnavailableException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class StudyGroupController {

	private final ObjectProvider<StudyGroupService> studyGroupService;

	@GetMapping(ApiPaths.V1 + "/groups")
	List<StudyGroupResponse> listGroups(Authentication authentication) {
		return service().listMyGroups(new ListStudyGroupsQuery(authenticatedUserId(authentication)))
			.stream()
			.map(StudyGroupResponse::from)
			.toList();
	}

	@PostMapping(ApiPaths.V1 + "/groups")
	@ResponseStatus(HttpStatus.CREATED)
	StudyGroupResponse createGroup(Authentication authentication, @Valid @RequestBody CreateGroupRequest request) {
		StudyGroupCreationResult result = service().createGroup(request.toCommand(authenticatedUserId(authentication)));
		return StudyGroupResponse.from(result.group());
	}

	@PostMapping(ApiPaths.V1 + "/groups/{groupId}/join")
	GroupMemberResponse joinGroup(
		Authentication authentication,
		@PathVariable UUID groupId,
		@Valid @RequestBody JoinGroupRequest request
	) {
		StudyGroupJoinResult result = service().joinGroup(request.toCommand(authenticatedUserId(authentication), groupId));
		return GroupMemberResponse.from(result.member());
	}

	private StudyGroupService service() {
		return studyGroupService.getIfAvailable(() -> {
			throw new StudyGroupServiceUnavailableException("study group service is not configured.");
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

	private record CreateGroupRequest(
		@NotBlank
		@Size(max = 120)
		String name,
		@NotBlank
		@Size(max = 120)
		String topic,
		@NotEmpty
		List<@NotBlank String> detailKeywords,
		@NotNull
		@Min(1)
		Integer maxMembers,
		@NotNull
		LocalDate startsAt,
		@NotNull
		LocalDate endsAt,
		String description
	) {

		@AssertTrue(message = "endsAt must be on or after startsAt")
		boolean isPeriodValid() {
			return startsAt == null || endsAt == null || !endsAt.isBefore(startsAt);
		}

		CreateStudyGroupCommand toCommand(UUID authenticatedUserId) {
			return new CreateStudyGroupCommand(
				authenticatedUserId,
				name,
				topic,
				detailKeywords,
				maxMembers,
				startsAt,
				endsAt,
				description
			);
		}
	}

	private record StudyGroupResponse(
		UUID id,
		String name,
		String topic,
		List<String> detailKeywords,
		StudyGroupStatus status,
		int maxMembers,
		String inviteCode,
		LocalDate startsAt,
		LocalDate endsAt
	) {

		private static StudyGroupResponse from(StudyGroup group) {
			return new StudyGroupResponse(
				group.id(),
				group.name(),
				group.topic(),
				group.detailKeywords(),
				group.status(),
				group.maxMembers(),
				group.inviteCode(),
				group.startsAt(),
				group.endsAt()
			);
		}
	}

	private record JoinGroupRequest(
		@NotBlank
		String inviteCode
	) {

		JoinStudyGroupCommand toCommand(UUID authenticatedUserId, UUID groupId) {
			return new JoinStudyGroupCommand(authenticatedUserId, groupId, inviteCode);
		}
	}

	private record GroupMemberResponse(
		UUID id,
		UUID groupId,
		UUID userId,
		GroupMemberPermission permission,
		GroupMemberStatus status,
		String displayName
	) {

		private static GroupMemberResponse from(GroupMember member) {
			return new GroupMemberResponse(
				member.id(),
				member.groupId(),
				member.userId(),
				member.permission(),
				member.status(),
				member.displayName().orElse(null)
			);
		}
	}
}
