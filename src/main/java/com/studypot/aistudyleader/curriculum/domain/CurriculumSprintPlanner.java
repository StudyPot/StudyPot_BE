package com.studypot.aistudyleader.curriculum.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CurriculumSprintPlanner {

	private static final Duration FIXED_WEEKLY_SPRINT_DURATION = Duration.ofDays(7);

	private CurriculumSprintPlanner() {
	}

	public static List<CurriculumSprintWindow> fixedWeeklyWindows(LocalDate startsAt, LocalDate endsAt) {
		Objects.requireNonNull(startsAt, "startsAt must not be null");
		Objects.requireNonNull(endsAt, "endsAt must not be null");
		if (endsAt.isBefore(startsAt)) {
			throw new IllegalArgumentException("endsAt must be on or after startsAt");
		}

		Instant studyEndExclusive = endsAt.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant windowStart = startsAt.atStartOfDay(ZoneOffset.UTC).toInstant();
		List<CurriculumSprintWindow> windows = new ArrayList<>();
		int weekNumber = 1;
		while (windowStart.isBefore(studyEndExclusive)) {
			Instant windowEnd = min(windowStart.plus(FIXED_WEEKLY_SPRINT_DURATION), studyEndExclusive);
			windows.add(new CurriculumSprintWindow(weekNumber, windowStart, windowEnd));
			weekNumber++;
			windowStart = windowEnd;
		}
		return List.copyOf(windows);
	}

	private static Instant min(Instant left, Instant right) {
		return left.isBefore(right) ? left : right;
	}
}
