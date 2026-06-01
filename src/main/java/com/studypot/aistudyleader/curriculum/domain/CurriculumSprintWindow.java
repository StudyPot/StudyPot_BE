package com.studypot.aistudyleader.curriculum.domain;

import java.time.Instant;
import java.util.Objects;

public record CurriculumSprintWindow(
	int weekNumber,
	Instant startsAt,
	Instant endsAt
) {

	public CurriculumSprintWindow {
		if (weekNumber <= 0) {
			throw new IllegalArgumentException("weekNumber must be positive");
		}
		Objects.requireNonNull(startsAt, "startsAt must not be null");
		Objects.requireNonNull(endsAt, "endsAt must not be null");
		if (!endsAt.isAfter(startsAt)) {
			throw new IllegalArgumentException("endsAt must be after startsAt");
		}
	}
}
