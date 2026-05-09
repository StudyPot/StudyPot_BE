package com.studypot.aistudyleader.studygroup.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreateStudyGroupCommand(
	UUID authenticatedUserId,
	String name,
	String topic,
	List<String> detailKeywords,
	int maxMembers,
	LocalDate startsAt,
	LocalDate endsAt,
	String description
) {

	public CreateStudyGroupCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(detailKeywords, "detailKeywords must not be null");
		detailKeywords = List.copyOf(detailKeywords);
		Objects.requireNonNull(startsAt, "startsAt must not be null");
		Objects.requireNonNull(endsAt, "endsAt must not be null");
	}
}
