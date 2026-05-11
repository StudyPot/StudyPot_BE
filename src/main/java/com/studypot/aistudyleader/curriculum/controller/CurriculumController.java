package com.studypot.aistudyleader.curriculum.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.curriculum.domain.Curriculum;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStatus;
import com.studypot.aistudyleader.curriculum.service.CurriculumService;
import com.studypot.aistudyleader.curriculum.service.CurriculumServiceUnavailableException;
import com.studypot.aistudyleader.curriculum.service.GetCurriculumQuery;
import com.studypot.aistudyleader.curriculum.service.StartCurriculumCommand;
import com.studypot.aistudyleader.global.api.ApiPaths;
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

@RestController
@RequiredArgsConstructor
class CurriculumController {

	private final ObjectProvider<CurriculumService> curriculumService;

	@PostMapping(ApiPaths.V1 + "/groups/{groupId}/start")
	@ResponseStatus(HttpStatus.CREATED)
	CurriculumResponse startStudy(Authentication authentication, @PathVariable UUID groupId) {
		Curriculum curriculum = service().startStudy(new StartCurriculumCommand(authenticatedUserId(authentication), groupId));
		return CurriculumResponse.from(curriculum);
	}

	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/curriculum")
	CurriculumResponse getCurriculum(Authentication authentication, @PathVariable UUID groupId) {
		Curriculum curriculum = service().getCurriculum(new GetCurriculumQuery(authenticatedUserId(authentication), groupId));
		return CurriculumResponse.from(curriculum);
	}

	private CurriculumService service() {
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

	private record CurriculumResponse(
		UUID id,
		UUID groupId,
		String title,
		int totalWeeks,
		Map<String, Object> onboardingSummary,
		CurriculumStatus status
	) {

		private static CurriculumResponse from(Curriculum curriculum) {
			return new CurriculumResponse(
				curriculum.id(),
				curriculum.groupId(),
				curriculum.title(),
				curriculum.totalWeeks(),
				curriculum.onboardingSummary(),
				curriculum.status()
			);
		}
	}
}
