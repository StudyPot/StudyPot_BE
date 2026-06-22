package com.studypot.aistudyleader.studygroup.service;

import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ListStudyGroupsQuery(
	UUID authenticatedUserId,
	StudyGroupStatus status,
	String query,
	String sortField,
	String sortOrder
) {

	public ListStudyGroupsQuery(UUID authenticatedUserId) {
		this(authenticatedUserId, null, null, null, null);
	}

	public ListStudyGroupsQuery {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		query = query == null || query.isBlank() ? null : query.strip();
	}

	public Optional<StudyGroupStatus> statusFilter() {
		return Optional.ofNullable(status);
	}

	public Optional<String> queryFilter() {
		return Optional.ofNullable(query);
	}

	public boolean descending() {
		return "desc".equalsIgnoreCase(sortOrder);
	}

	public static StudyGroupStatus parseStatus(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return StudyGroupStatus.valueOf(value.strip().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}
}
