package com.studypot.aistudyleader.curriculum.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CurriculumSprintPlannerTest {

	@Test
	void fixedWeeklyWindowsUseStudyDatesAtUtcDayBoundaries() {
		List<CurriculumSprintWindow> windows = CurriculumSprintPlanner.fixedWeeklyWindows(
			LocalDate.parse("2026-06-01"),
			LocalDate.parse("2026-06-21")
		);

		assertThat(windows).hasSize(3);
		assertThat(windows)
			.extracting(CurriculumSprintWindow::weekNumber)
			.containsExactly(1, 2, 3);
		assertThat(windows)
			.extracting(CurriculumSprintWindow::startsAt)
			.containsExactly(
				Instant.parse("2026-06-01T00:00:00Z"),
				Instant.parse("2026-06-08T00:00:00Z"),
				Instant.parse("2026-06-15T00:00:00Z")
			);
		assertThat(windows)
			.extracting(CurriculumSprintWindow::endsAt)
			.containsExactly(
				Instant.parse("2026-06-08T00:00:00Z"),
				Instant.parse("2026-06-15T00:00:00Z"),
				Instant.parse("2026-06-22T00:00:00Z")
			);
	}

	@Test
	void fixedWeeklyWindowsCapsFinalPartialWeekAtStudyEndExclusive() {
		List<CurriculumSprintWindow> windows = CurriculumSprintPlanner.fixedWeeklyWindows(
			LocalDate.parse("2026-06-01"),
			LocalDate.parse("2026-06-10")
		);

		assertThat(windows).hasSize(2);
		assertThat(windows.get(1).startsAt()).isEqualTo(Instant.parse("2026-06-08T00:00:00Z"));
		assertThat(windows.get(1).endsAt()).isEqualTo(Instant.parse("2026-06-11T00:00:00Z"));
	}

	@Test
	void fixedWeeklyWindowsSupportsPeriodsLongerThanOneYear() {
		List<CurriculumSprintWindow> windows = CurriculumSprintPlanner.fixedWeeklyWindows(
			LocalDate.parse("2026-01-01"),
			LocalDate.parse("2027-01-01")
		);

		assertThat(windows).hasSize(53);
		assertThat(windows.getLast().weekNumber()).isEqualTo(53);
		assertThat(windows.getLast().startsAt()).isEqualTo(Instant.parse("2026-12-31T00:00:00Z"));
		assertThat(windows.getLast().endsAt()).isEqualTo(Instant.parse("2027-01-02T00:00:00Z"));
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
