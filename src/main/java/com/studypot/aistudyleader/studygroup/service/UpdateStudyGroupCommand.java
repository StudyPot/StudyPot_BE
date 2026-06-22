package com.studypot.aistudyleader.studygroup.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record UpdateStudyGroupCommand(
	UUID authenticatedUserId,
	UUID groupId,
	String name,
	String topic,
	List<String> detailKeywords,
	int maxMembers,
	LocalDate startsAt,
	LocalDate endsAt,
	String description
) {

	public UpdateStudyGroupCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		detailKeywords = detailKeywords == null ? List.of() : List.copyOf(detailKeywords);
	}
}
