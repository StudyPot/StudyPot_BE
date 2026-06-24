package com.studypot.aistudyleader.curriculum.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studypot.aistudyleader.notification.service.NotificationEventPublisher;
import com.studypot.aistudyleader.report.service.StudyCompletionReportTrigger;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class WeekLifecycleSchedulerTest {

	private static final Instant NOW = Instant.parse("2026-06-23T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final NotificationEventPublisher publisher = mock(NotificationEventPublisher.class);
	@SuppressWarnings("unchecked")
	private final ObjectProvider<StudyCompletionReportTrigger> completionReportTrigger = mock(ObjectProvider.class);
	private final WeekLifecycleScheduler scheduler =
		new WeekLifecycleScheduler(jdbcTemplate, publisher, completionReportTrigger, CLOCK);

	@Test
	void advanceWeeksCompletesEndedWeeks() {
		scheduler.advanceWeeks();

		verify(jdbcTemplate).update(eq(WeekLifecycleScheduler.COMPLETE_ENDED_WEEKS), any(Object[].class));
	}

	@Test
	void advanceWeeksActivatesStartedWeekAndPublishesWeekStarted() {
		UUID groupId = UUID.fromString("018f0000-0000-7000-8000-000000000a01");
		UUID weekId = UUID.fromString("018f0000-0000-7000-8000-000000000a02");
		when(jdbcTemplate.query(eq(WeekLifecycleScheduler.SELECT_WEEKS_TO_ACTIVATE), any(RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(new WeekLifecycleScheduler.Activation(groupId, weekId, 2, "2주차")));
		when(jdbcTemplate.update(eq(WeekLifecycleScheduler.ACTIVATE_WEEK), any(Object[].class))).thenReturn(1);

		scheduler.advanceWeeks();

		// 마감 처리와 활성화가 한 틱에 함께 수행된다.
		verify(jdbcTemplate).update(eq(WeekLifecycleScheduler.COMPLETE_ENDED_WEEKS), any(Object[].class));
		verify(jdbcTemplate).update(eq(WeekLifecycleScheduler.ACTIVATE_WEEK), any(Object[].class));
		verify(publisher).publishWeekStarted(groupId, weekId, 2, "2주차");
	}

	@Test
	void advanceWeeksActivatesMultipleWeeks() {
		UUID groupId = UUID.fromString("018f0000-0000-7000-8000-000000000a21");
		UUID weekA = UUID.fromString("018f0000-0000-7000-8000-000000000a22");
		UUID weekB = UUID.fromString("018f0000-0000-7000-8000-000000000a23");
		when(jdbcTemplate.query(eq(WeekLifecycleScheduler.SELECT_WEEKS_TO_ACTIVATE), any(RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(
				new WeekLifecycleScheduler.Activation(groupId, weekA, 2, "2주차"),
				new WeekLifecycleScheduler.Activation(groupId, weekB, 3, "3주차")
			));
		when(jdbcTemplate.update(eq(WeekLifecycleScheduler.ACTIVATE_WEEK), any(Object[].class))).thenReturn(1);

		scheduler.advanceWeeks();

		verify(jdbcTemplate, times(2)).update(eq(WeekLifecycleScheduler.ACTIVATE_WEEK), any(Object[].class));
		verify(publisher).publishWeekStarted(groupId, weekA, 2, "2주차");
		verify(publisher).publishWeekStarted(groupId, weekB, 3, "3주차");
	}

	@Test
	void advanceWeeksHandlesEmptyActivationSet() {
		when(jdbcTemplate.query(eq(WeekLifecycleScheduler.SELECT_WEEKS_TO_ACTIVATE), any(RowMapper.class), any(Object[].class)))
			.thenReturn(List.of());

		scheduler.advanceWeeks();

		verify(jdbcTemplate, never()).update(eq(WeekLifecycleScheduler.ACTIVATE_WEEK), any(Object[].class));
		verify(publisher, never()).publishWeekStarted(any(), any(), anyInt(), any());
	}

	@Test
	void advanceWeeksDoesNotPublishWhenActivationLosesRace() {
		UUID groupId = UUID.fromString("018f0000-0000-7000-8000-000000000a11");
		UUID weekId = UUID.fromString("018f0000-0000-7000-8000-000000000a12");
		when(jdbcTemplate.query(eq(WeekLifecycleScheduler.SELECT_WEEKS_TO_ACTIVATE), any(RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(new WeekLifecycleScheduler.Activation(groupId, weekId, 2, "2주차")));
		when(jdbcTemplate.update(eq(WeekLifecycleScheduler.ACTIVATE_WEEK), any(Object[].class))).thenReturn(0);

		scheduler.advanceWeeks();

		verify(publisher, never()).publishWeekStarted(any(), any(), anyInt(), any());
	}

	@Test
	void advanceWeeksSwallowsNotificationFailures() {
		UUID groupId = UUID.fromString("018f0000-0000-7000-8000-000000000a31");
		UUID weekId = UUID.fromString("018f0000-0000-7000-8000-000000000a32");
		when(jdbcTemplate.query(eq(WeekLifecycleScheduler.SELECT_WEEKS_TO_ACTIVATE), any(RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(new WeekLifecycleScheduler.Activation(groupId, weekId, 2, "2주차")));
		when(jdbcTemplate.update(eq(WeekLifecycleScheduler.ACTIVATE_WEEK), any(Object[].class))).thenReturn(1);
		doThrow(new RuntimeException("notify boom")).when(publisher).publishWeekStarted(groupId, weekId, 2, "2주차");

		assertThatCode(scheduler::advanceWeeks).doesNotThrowAnyException();
	}
}
