package com.studypot.aistudyleader.curriculum.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record SubmittedOnboardingResponse(
	UUID id,
	UUID memberId,
	Map<String, Integer> keywordSkillLevels,
	Map<String, Integer> taskPreferences,
	String additionalNote,
	List<SubmittedAvailabilitySlot> availabilitySlots,
	Instant submittedAt
) {

	public SubmittedOnboardingResponse {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		keywordSkillLevels = Map.copyOf(Objects.requireNonNull(keywordSkillLevels, "keywordSkillLevels must not be null"));
		taskPreferences = Map.copyOf(Objects.requireNonNull(taskPreferences, "taskPreferences must not be null"));
		additionalNote = additionalNote == null || additionalNote.isBlank() ? null : additionalNote.strip();
		availabilitySlots = List.copyOf(Objects.requireNonNull(availabilitySlots, "availabilitySlots must not be null"));
		Objects.requireNonNull(submittedAt, "submittedAt must not be null");
	}
}
