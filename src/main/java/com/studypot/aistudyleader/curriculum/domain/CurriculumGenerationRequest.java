package com.studypot.aistudyleader.curriculum.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record CurriculumGenerationRequest(
	CurriculumStartContext group,
	List<SubmittedOnboardingResponse> submittedResponses,
	Map<String, Object> onboardingSummary,
	Instant requestedAt
) {

	public CurriculumGenerationRequest {
		Objects.requireNonNull(group, "group must not be null");
		submittedResponses = List.copyOf(Objects.requireNonNull(submittedResponses, "submittedResponses must not be null"));
		onboardingSummary = Map.copyOf(Objects.requireNonNull(onboardingSummary, "onboardingSummary must not be null"));
		Objects.requireNonNull(requestedAt, "requestedAt must not be null");
	}
}
