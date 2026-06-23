package com.studypot.aistudyleader.summary.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.curriculum.service.CurriculumService;
import com.studypot.aistudyleader.curriculum.service.CurriculumServiceUnavailableException;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.studygroup.service.StudyGroupService;
import com.studypot.aistudyleader.studygroup.service.StudyGroupServiceUnavailableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "내 요약", description = "전체 그룹 화면 상단의 내 그룹 수·이번 주 활동 수 요약을 제공하는 API입니다.")
@RestController
@RequiredArgsConstructor
class GroupSummaryController {

	private final ObjectProvider<StudyGroupService> studyGroupService;
	private final ObjectProvider<CurriculumService> curriculumService;

	@Operation(
		summary = "내 그룹 요약 조회",
		description = "인증된 사용자의 참여 그룹 수와 최근 7일 동안 완료한 활동(todo) 수를 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "내 그룹 요약 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "503", description = "필요한 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/summary")
	GroupSummaryResponse getMySummary(Authentication authentication) {
		UUID userId = authenticatedUserId(authentication);
		int groupCount = studyGroupService().countMyGroups(userId);
		int weeklyActivityCount = curriculumService().countMyWeeklyDoneActivity(userId);
		return new GroupSummaryResponse(groupCount, weeklyActivityCount);
	}

	private StudyGroupService studyGroupService() {
		return studyGroupService.getIfAvailable(() -> {
			throw new StudyGroupServiceUnavailableException("study group service is not configured.");
		});
	}

	private CurriculumService curriculumService() {
		return curriculumService.getIfAvailable(() -> {
			throw new CurriculumServiceUnavailableException("curriculum service is not configured.");
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

	@Schema(description = "내 그룹 요약 응답입니다.")
	private record GroupSummaryResponse(
		@Schema(description = "참여 중인 그룹 수입니다.", example = "3")
		int groupCount,
		@Schema(description = "최근 7일 동안 완료한 활동(todo) 수입니다.", example = "12")
		int weeklyActivityCount
	) {
	}
}
