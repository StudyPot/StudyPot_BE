package com.studypot.aistudyleader.curriculum.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studypot.aistudyleader.notification.service.NotificationEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class WeekLifecycleSchedulerTest {

	private static final Instant NOW = Instant.parse("2026-06-23T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final NotificationEventPublisher publisher = mock(NotificationEventPublisher.class);
	private final WeekLifecycleScheduler scheduler = new WeekLifecycleScheduler(jdbcTemplate, publisher, CLOCK);

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

		verify(jdbcTemplate).update(eq(WeekLifecycleScheduler.ACTIVATE_WEEK), any(Object[].class));
		verify(publisher).publishWeekStarted(groupId, weekId, 2, "2주차");
	}

	@Test
	void advanceWeeksDoesNotPublishWhenActivationLosesRace() {
		UUID groupId = UUID.fromString("018f0000-0000-7000-8000-000000000a11");
		UUID weekId = UUID.fromString("018f0000-0000-7000-8000-000000000a12");
		when(jdbcTemplate.query(eq(WeekLifecycleScheduler.SELECT_WEEKS_TO_ACTIVATE), any(RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(new WeekLifecycleScheduler.Activation(groupId, weekId, 2, "2주차")));
		when(jdbcTemplate.update(eq(WeekLifecycleScheduler.ACTIVATE_WEEK), any(Object[].class))).thenReturn(0);

		scheduler.advanceWeeks();

		verify(publisher, never()).publishWeekStarted(any(), any(), org.mockito.ArgumentMatchers.anyInt(), any());
	}
}
