package com.studypot.aistudyleader.onboarding.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SubmitMyOnboardingCommand(
	UUID authenticatedUserId,
	UUID groupId,
	int skillLevel,
	String additionalNote,
	List<AvailabilitySlotCommand> availabilitySlots
) {

	public SubmitMyOnboardingCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(availabilitySlots, "availabilitySlots must not be null");
		additionalNote = additionalNote == null || additionalNote.isBlank() ? null : additionalNote.strip();
		availabilitySlots = List.copyOf(availabilitySlots);
	}
}
