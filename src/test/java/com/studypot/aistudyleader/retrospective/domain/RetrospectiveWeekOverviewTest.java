package com.studypot.aistudyleader.retrospective.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RetrospectiveWeekOverviewTest {

	@Test
	@DisplayName("시작 전(PENDING) 주차는 잠긴다")
	void pendingWeekIsLocked() {
		assertThat(RetrospectiveWeekOverview.unlocked("PENDING", 3, 3, false)).isFalse();
	}

	@Test
	@DisplayName("진행 중 + 필수 TODO 전부 완료면 작성 가능")
	void inProgressWithAllRequiredDoneIsUnlocked() {
		assertThat(RetrospectiveWeekOverview.unlocked("IN_PROGRESS", 3, 3, false)).isTrue();
	}

	@Test
	@DisplayName("진행 중인데 필수 TODO 가 남았으면 잠긴다")
	void inProgressWithRemainingRequiredIsLocked() {
		assertThat(RetrospectiveWeekOverview.unlocked("IN_PROGRESS", 3, 2, false)).isFalse();
	}

	@Test
	@DisplayName("주차가 종료되면 미완료여도 리포트 전까지 작성 가능")
	void endedBeforeReportIsUnlockedEvenIfIncomplete() {
		assertThat(RetrospectiveWeekOverview.unlocked("COMPLETED", 3, 0, false)).isTrue();
	}

	@Test
	@DisplayName("리포트가 게시되면 닫힌다 — 종료/완료 여부와 무관")
	void reportPostedClosesTheWeek() {
		assertThat(RetrospectiveWeekOverview.unlocked("COMPLETED", 3, 0, true)).isFalse();
		assertThat(RetrospectiveWeekOverview.unlocked("COMPLETED", 3, 3, true)).isFalse();
		assertThat(RetrospectiveWeekOverview.unlocked("IN_PROGRESS", 3, 3, true)).isFalse();
	}

	@Test
	@DisplayName("필수 TODO 가 없는 주차는 시작되면(진행/종료) 리포트 전까지 작성 가능")
	void noRequiredTasksUnlockOnceStarted() {
		assertThat(RetrospectiveWeekOverview.unlocked("IN_PROGRESS", 0, 0, false)).isTrue();
		assertThat(RetrospectiveWeekOverview.unlocked("PENDING", 0, 0, false)).isFalse();
	}
}
