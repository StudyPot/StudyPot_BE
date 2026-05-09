package com.studypot.aistudyleader.onboarding.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.onboarding.domain.GroupOnboardingResponse;
import com.studypot.aistudyleader.onboarding.domain.GroupOnboardingStatus;
import com.studypot.aistudyleader.onboarding.service.GetMyOnboardingQuery;
import com.studypot.aistudyleader.onboarding.service.OnboardingService;
import com.studypot.aistudyleader.onboarding.service.OnboardingServiceUnavailableException;
import com.studypot.aistudyleader.onboarding.service.SaveMyOnboardingCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class OnboardingController {

	private final ObjectProvider<OnboardingService> onboardingService;

	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/onboarding/me")
	OnboardingResponse getMyOnboarding(Authentication authentication, @PathVariable UUID groupId) {
		GroupOnboardingResponse response = service().getMyResponse(new GetMyOnboardingQuery(authenticatedUserId(authentication), groupId));
		return OnboardingResponse.from(response);
	}

	@PutMapping(ApiPaths.V1 + "/groups/{groupId}/onboarding/me")
	OnboardingResponse saveMyOnboarding(
		Authentication authentication,
		@PathVariable UUID groupId,
		@Valid @RequestBody SaveOnboardingRequest request
	) {
		GroupOnboardingResponse response = service().saveMyDraft(request.toCommand(authenticatedUserId(authentication), groupId));
		return OnboardingResponse.from(response);
	}

	private OnboardingService service() {
		return onboardingService.getIfAvailable(() -> {
			throw new OnboardingServiceUnavailableException("onboarding service is not configured.");
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

	private record SaveOnboardingRequest(
		@NotNull
		Map<String, Integer> keywordSkillLevels,
		@NotNull
		Map<String, Integer> taskPreferences,
		String additionalNote,
		@NotNull
		List<Map<String, Object>> availabilitySlots
	) {

		@AssertTrue(message = "availabilitySlots are handled by a follow-up task")
		boolean isAvailabilitySlotsEmpty() {
			return availabilitySlots == null || availabilitySlots.isEmpty();
		}

		SaveMyOnboardingCommand toCommand(UUID authenticatedUserId, UUID groupId) {
			return new SaveMyOnboardingCommand(authenticatedUserId, groupId, keywordSkillLevels, taskPreferences, additionalNote);
		}
	}

	private record OnboardingResponse(
		UUID id,
		UUID groupId,
		UUID memberId,
		Map<String, Integer> keywordSkillLevels,
		Map<String, Integer> taskPreferences,
		String additionalNote,
		List<Object> availabilitySlots,
		GroupOnboardingStatus status,
		Instant submittedAt
	) {

		private static OnboardingResponse from(GroupOnboardingResponse response) {
			return new OnboardingResponse(
				response.id(),
				response.groupId(),
				response.memberId(),
				response.keywordSkillLevels(),
				response.taskPreferences(),
				response.additionalNote().orElse(null),
				List.of(),
				response.status(),
				response.submittedAt().orElse(null)
			);
		}
	}
}
