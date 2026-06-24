package com.studypot.aistudyleader.curriculum.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CurriculumSprintPlannerTest {

	@Test
	void fixedWeeklyWindowsUseKstStartAndLastDayEndTime() {
		List<CurriculumSprintWindow> windows = CurriculumSprintPlanner.fixedWeeklyWindows(
			LocalDate.parse("2026-06-01"),
			LocalDate.parse("2026-06-21")
		);

		assertThat(windows).hasSize(3);
		assertThat(windows)
			.extracting(CurriculumSprintWindow::weekNumber)
			.containsExactly(1, 2, 3);
		// 각 주차는 첫째 날 00:00(KST) 시작 = 전날 15:00 UTC
		assertThat(windows)
			.extracting(CurriculumSprintWindow::startsAt)
			.containsExactly(
				Instant.parse("2026-05-31T15:00:00Z"),
				Instant.parse("2026-06-07T15:00:00Z"),
				Instant.parse("2026-06-14T15:00:00Z")
			);
		// 각 주차는 마지막 날 23:30(KST) 종료 = 그 날 14:30 UTC
		assertThat(windows)
			.extracting(CurriculumSprintWindow::endsAt)
			.containsExactly(
				Instant.parse("2026-06-07T14:30:00Z"),
				Instant.parse("2026-06-14T14:30:00Z"),
				Instant.parse("2026-06-21T14:30:00Z")
			);
	}

	@Test
	void fixedWeeklyWindowsCapsFinalPartialWeekAtStudyEndDate() {
		List<CurriculumSprintWindow> windows = CurriculumSprintPlanner.fixedWeeklyWindows(
			LocalDate.parse("2026-06-01"),
			LocalDate.parse("2026-06-10")
		);

		assertThat(windows).hasSize(2);
		assertThat(windows.get(1).startsAt()).isEqualTo(Instant.parse("2026-06-07T15:00:00Z"));
		// 마지막 부분 주차는 스터디 종료일(6/10) 23:30 KST 까지
		assertThat(windows.get(1).endsAt()).isEqualTo(Instant.parse("2026-06-10T14:30:00Z"));
	}

	@Test
	void fixedWeeklyWindowsSupportsPeriodsLongerThanOneYear() {
		List<CurriculumSprintWindow> windows = CurriculumSprintPlanner.fixedWeeklyWindows(
			LocalDate.parse("2026-01-01"),
			LocalDate.parse("2027-01-01")
		);

		assertThat(windows).hasSize(53);
		assertThat(windows.getLast().weekNumber()).isEqualTo(53);
		assertThat(windows.getLast().startsAt()).isEqualTo(Instant.parse("2026-12-30T15:00:00Z"));
		assertThat(windows.getLast().endsAt()).isEqualTo(Instant.parse("2027-01-01T14:30:00Z"));
	}

	@Test
	void fixedWeeklyWindowsRejectsInvalidStudyPeriod() {
		assertThatThrownBy(() -> CurriculumSprintPlanner.fixedWeeklyWindows(
				LocalDate.parse("2026-06-10"),
				LocalDate.parse("2026-06-01")
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("endsAt must be on or after startsAt");
	}
}
