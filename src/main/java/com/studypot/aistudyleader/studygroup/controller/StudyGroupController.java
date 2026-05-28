package com.studypot.aistudyleader.studygroup.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import com.studypot.aistudyleader.studygroup.service.CreateStudyGroupCommand;
import com.studypot.aistudyleader.studygroup.service.DetailKeywordSuggestions;
import com.studypot.aistudyleader.studygroup.service.DetailKeywordSuggestionService;
import com.studypot.aistudyleader.studygroup.service.GetStudyGroupQuery;
import com.studypot.aistudyleader.studygroup.service.JoinStudyGroupCommand;
import com.studypot.aistudyleader.studygroup.service.ListStudyGroupsQuery;
import com.studypot.aistudyleader.studygroup.service.SuggestDetailKeywordsCommand;
import com.studypot.aistudyleader.studygroup.service.StudyGroupCreationResult;
import com.studypot.aistudyleader.studygroup.service.StudyGroupJoinResult;
import com.studypot.aistudyleader.studygroup.service.StudyGroupService;
import com.studypot.aistudyleader.studygroup.service.StudyGroupServiceUnavailableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
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

@Tag(name = "스터디 그룹", description = "스터디 그룹 생성, 내 그룹 목록 조회, 초대 코드 기반 참여를 제공하는 API입니다.")
@RestController
@RequiredArgsConstructor
class StudyGroupController {

	private static final int DEFAULT_DETAIL_KEYWORD_MAX_CANDIDATES = 5;

	private final ObjectProvider<StudyGroupService> studyGroupService;
	private final ObjectProvider<DetailKeywordSuggestionService> detailKeywordSuggestionService;

	@Operation(
		summary = "내 스터디 그룹 목록 조회",
		description = "인증된 사용자가 현재 참여 중이거나 온보딩 대기 중인 스터디 그룹 목록을 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "내 그룹 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "503", description = "스터디 그룹 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups")
	List<StudyGroupResponse> listGroups(Authentication authentication) {
		return service().listMyGroups(new ListStudyGroupsQuery(authenticatedUserId(authentication)))
			.stream()
			.map(StudyGroupResponse::from)
			.toList();
	}

	@Operation(
		summary = "스터디 그룹 생성",
		description = "그룹 이름, 주제, 세부 키워드, 모집 인원, 진행 기간을 받아 새 스터디 그룹과 소유자 멤버십을 생성합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "스터디 그룹이 생성되고 소유자 멤버십이 함께 생성됨"),
		@ApiResponse(responseCode = "400", description = "필수 값이 비어 있거나 기간/인원 검증에 실패함"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "503", description = "스터디 그룹 서비스가 아직 구성되지 않음")
	})
	@PostMapping(ApiPaths.V1 + "/groups")
	@ResponseStatus(HttpStatus.CREATED)
	StudyGroupResponse createGroup(Authentication authentication, @Valid @RequestBody CreateGroupRequest request) {
		StudyGroupCreationResult result = service().createGroup(request.toCommand(authenticatedUserId(authentication)));
		return StudyGroupResponse.from(result.group());
	}

	@Operation(
		summary = "스터디 그룹 상세 조회",
		description = "인증된 그룹 멤버가 스터디 그룹 기본 정보를 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "그룹 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
		@ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "스터디 그룹 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}")
	StudyGroupResponse getGroup(
		Authentication authentication,
		@Parameter(description = "조회하려는 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		StudyGroup group = service().getGroup(new GetStudyGroupQuery(authenticatedUserId(authentication), groupId));
		return StudyGroupResponse.from(group);
	}

	@Operation(
		summary = "AI 세부 키워드 추천",
		description = "스터디 그룹 생성 전 큰 주제와 선택적 힌트를 받아 선택 가능한 세부 키워드 후보를 추천합니다. 추천 후보는 저장되지 않습니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "추천 키워드 후보 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "422", description = "필수 값이 비어 있거나 후보 개수 검증에 실패함"),
		@ApiResponse(responseCode = "503", description = "AI 추천 서비스가 아직 구성되지 않음")
	})
	@PostMapping(ApiPaths.V1 + "/groups/detail-keyword-suggestions")
	DetailKeywordSuggestionsResponse suggestDetailKeywords(
		Authentication authentication,
		@Valid @RequestBody SuggestDetailKeywordsRequest request
	) {
		DetailKeywordSuggestions suggestions = detailKeywordService()
			.suggest(request.toCommand(authenticatedUserId(authentication)));
		return DetailKeywordSuggestionsResponse.from(suggestions);
	}

	@Operation(
		summary = "초대 코드로 그룹 참여",
		description = "스터디 그룹 ID와 초대 코드를 검증해 인증된 사용자를 온보딩 대기 멤버로 추가합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "그룹 멤버십 생성 또는 기존 멤버십 반환"),
		@ApiResponse(responseCode = "400", description = "초대 코드가 비어 있거나 요청 형식이 올바르지 않음"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "409", description = "정원 초과 또는 중복 참여처럼 참여 조건을 만족하지 못함"),
		@ApiResponse(responseCode = "503", description = "스터디 그룹 서비스가 아직 구성되지 않음")
	})
	@PostMapping(ApiPaths.V1 + "/groups/{groupId}/join")
	GroupMemberResponse joinGroup(
		Authentication authentication,
		@Parameter(description = "참여하려는 스터디 그룹 UUID입니다.", required = true)
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

	private DetailKeywordSuggestionService detailKeywordService() {
		return detailKeywordSuggestionService.getIfAvailable(() -> {
			throw new StudyGroupServiceUnavailableException("detail keyword suggestion service is not configured.");
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

	@Schema(description = "새 스터디 그룹 생성을 위한 요청입니다.")
	private record CreateGroupRequest(
		@Schema(description = "스터디 그룹 이름입니다. 화면과 목록에서 대표 이름으로 표시됩니다.", example = "백엔드 인터뷰 스터디")
		@NotBlank
		@Size(max = 120)
		String name,
		@Schema(description = "스터디의 큰 주제입니다.", example = "Spring Boot")
		@NotBlank
		@Size(max = 120)
		String topic,
		@Schema(description = "스터디에서 집중할 세부 키워드 목록입니다.", example = "[\"JPA\", \"Security\", \"Testing\"]")
		@NotEmpty
		List<@NotBlank String> detailKeywords,
		@Schema(description = "그룹에 참여할 수 있는 최대 인원입니다.", example = "6")
		@NotNull
		@Min(1)
		Integer maxMembers,
		@Schema(description = "스터디 시작일입니다.", example = "2026-05-18")
		@NotNull
		LocalDate startsAt,
		@Schema(description = "스터디 종료일입니다. 시작일과 같거나 이후여야 합니다.", example = "2026-06-29")
		@NotNull
		LocalDate endsAt,
		@Schema(description = "그룹 소개 또는 운영 메모입니다.", example = "매주 실습 과제와 코드 리뷰를 함께 진행합니다.")
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

	@Schema(description = "AI 세부 키워드 추천 요청입니다.")
	private record SuggestDetailKeywordsRequest(
		@Schema(description = "스터디의 큰 주제입니다.", example = "Spring Boot")
		@NotBlank
		@Size(max = 120)
		String topic,
		@Schema(description = "이미 고려 중인 키워드 힌트입니다. 생략하면 빈 목록으로 처리됩니다.", example = "[\"Backend\"]")
		@Size(max = 10)
		List<@NotBlank String> hintKeywords,
		@Schema(description = "추천받을 최대 후보 개수입니다. 생략하면 5개입니다.", example = "5")
		@Min(1)
		@Max(10)
		Integer maxCandidates
	) {

		SuggestDetailKeywordsCommand toCommand(UUID authenticatedUserId) {
			return new SuggestDetailKeywordsCommand(
				authenticatedUserId,
				topic,
				hintKeywords == null ? List.of() : hintKeywords,
				maxCandidates == null ? DEFAULT_DETAIL_KEYWORD_MAX_CANDIDATES : maxCandidates
			);
		}
	}

	@Schema(description = "AI 세부 키워드 추천 응답입니다.")
	private record DetailKeywordSuggestionsResponse(
		@Schema(description = "선택 가능한 세부 키워드 후보 목록입니다.", example = "[\"JPA\", \"Spring Security\", \"Spring Batch\"]")
		List<String> keywords
	) {

		private static DetailKeywordSuggestionsResponse from(DetailKeywordSuggestions suggestions) {
			return new DetailKeywordSuggestionsResponse(suggestions.keywords());
		}
	}

	@Schema(description = "스터디 그룹의 공개/멤버용 요약 응답입니다.")
	private record StudyGroupResponse(
		@Schema(description = "스터디 그룹 UUID입니다.", example = "018f6f55-6fb1-7d62-a711-25f7c6d16a28")
		UUID id,
		@Schema(description = "스터디 그룹 이름입니다.", example = "백엔드 인터뷰 스터디")
		String name,
		@Schema(description = "스터디의 큰 주제입니다.", example = "Spring Boot")
		String topic,
		@Schema(description = "스터디 세부 키워드 목록입니다.", example = "[\"JPA\", \"Security\", \"Testing\"]")
		List<String> detailKeywords,
		@Schema(description = "그룹 진행 상태입니다.", example = "ONBOARDING")
		StudyGroupStatus status,
		@Schema(description = "최대 참여 가능 인원입니다.", example = "6")
		int maxMembers,
		@Schema(description = "초대 링크/참여 요청에 사용하는 그룹 초대 코드입니다.", example = "SPRING-AB12")
		String inviteCode,
		@Schema(description = "스터디 시작일입니다.", example = "2026-05-18")
		LocalDate startsAt,
		@Schema(description = "스터디 종료일입니다.", example = "2026-06-29")
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

	@Schema(description = "초대 코드로 스터디 그룹에 참여하기 위한 요청입니다.")
	private record JoinGroupRequest(
		@Schema(description = "그룹장이 공유한 초대 코드입니다.", example = "SPRING-AB12")
		@NotBlank
		String inviteCode
	) {

		JoinStudyGroupCommand toCommand(UUID authenticatedUserId, UUID groupId) {
			return new JoinStudyGroupCommand(authenticatedUserId, groupId, inviteCode);
		}
	}

	@Schema(description = "스터디 그룹 멤버십 생성/조회 응답입니다.")
	private record GroupMemberResponse(
		@Schema(description = "그룹 멤버십 UUID입니다.", example = "018f6f55-75e9-78d2-9f5c-598945b93400")
		UUID id,
		@Schema(description = "멤버가 속한 스터디 그룹 UUID입니다.", example = "018f6f55-6fb1-7d62-a711-25f7c6d16a28")
		UUID groupId,
		@Schema(description = "멤버로 참여한 사용자 UUID입니다.", example = "018f6f55-6f42-7e11-b479-120c5f2e9d42")
		UUID userId,
		@Schema(description = "그룹 내 권한입니다.", example = "MEMBER")
		GroupMemberPermission permission,
		@Schema(description = "그룹 참여/온보딩 상태입니다.", example = "PENDING_ONBOARDING")
		GroupMemberStatus status,
		@Schema(description = "그룹 안에서 표시할 멤버 이름입니다.", example = "현우")
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
