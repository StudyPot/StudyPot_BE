package com.studypot.aistudyleader.onboarding.service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record SaveMyOnboardingCommand(
	UUID authenticatedUserId,
	UUID groupId,
	Map<String, Integer> keywordSkillLevels,
	Map<String, Integer> taskPreferences,
	String additionalNote
) {

	public SaveMyOnboardingCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(keywordSkillLevels, "keywordSkillLevels must not be null");
		Objects.requireNonNull(taskPreferences, "taskPreferences must not be null");
		keywordSkillLevels = Map.copyOf(keywordSkillLevels);
		taskPreferences = Map.copyOf(taskPreferences);
		additionalNote = additionalNote == null || additionalNote.isBlank() ? null : additionalNote.strip();
	}
}
