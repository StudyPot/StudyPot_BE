package com.studypot.aistudyleader.curriculum.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CurriculumSprintPlanner {

	private static final int DAYS_PER_WEEK = 7;
	// 주차 일정은 한국 시간(KST) 기준으로 계산한다.
	private static final ZoneId STUDY_ZONE = ZoneId.of("Asia/Seoul");
	// 한 주차는 그 주의 첫째 날 00:00(KST)에 시작해, 마지막 날 23:30(KST)에 끝난다.
	private static final LocalTime WEEK_END_TIME = LocalTime.of(23, 30);

	private CurriculumSprintPlanner() {
	}

	public static List<CurriculumSprintWindow> fixedWeeklyWindows(LocalDate startsAt, LocalDate endsAt) {
		Objects.requireNonNull(startsAt, "startsAt must not be null");
		Objects.requireNonNull(endsAt, "endsAt must not be null");
		if (endsAt.isBefore(startsAt)) {
			throw new IllegalArgumentException("endsAt must be on or after startsAt");
		}

		List<CurriculumSprintWindow> windows = new ArrayList<>();
		int weekNumber = 1;
		LocalDate weekStartDate = startsAt;
		while (!weekStartDate.isAfter(endsAt)) {
			LocalDate weekLastDate = weekStartDate.plusDays(DAYS_PER_WEEK - 1L);
			if (weekLastDate.isAfter(endsAt)) {
				weekLastDate = endsAt; // 마지막 주차는 스터디 종료일까지만(부분 주차)
			}
			Instant windowStart = weekStartDate.atStartOfDay(STUDY_ZONE).toInstant();
			Instant windowEnd = weekLastDate.atTime(WEEK_END_TIME).atZone(STUDY_ZONE).toInstant();
			windows.add(new CurriculumSprintWindow(weekNumber, windowStart, windowEnd));
			weekNumber++;
			weekStartDate = weekStartDate.plusDays(DAYS_PER_WEEK);
		}
		return List.copyOf(windows);
	}
}
