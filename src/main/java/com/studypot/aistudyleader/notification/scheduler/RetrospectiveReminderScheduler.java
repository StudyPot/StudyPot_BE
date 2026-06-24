package com.studypot.aistudyleader.notification.scheduler;

import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.notification.service.NotificationEventPublisher;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 진행 중인 주차의 마감 1시간 전에 활성 멤버에게 회고 작성 리마인더(SSE/인앱 알림)를 보낸다.
 * 중복 발송은 알림 멱등키(week+recipient)로 방지된다.
 */
@Component
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
class RetrospectiveReminderScheduler {

	private static final Logger log = LoggerFactory.getLogger(RetrospectiveReminderScheduler.class);

	private static final Duration REMINDER_WINDOW = Duration.ofHours(1);

	private static final String SELECT_DUE_SOON_REMINDERS = """
		select c.group_id, cw.id as week_id, gm.user_id
		from curriculum_week cw
		join curriculum c on c.id = cw.curriculum_id
		join group_member gm on gm.group_id = c.group_id
		where cw.status = 'IN_PROGRESS'
		  and c.status = 'ACTIVE'
		  and cw.deleted_at is null
		  and c.deleted_at is null
		  and gm.status = 'ACTIVE'
		  and gm.deleted_at is null
		  and cw.ends_at > ?
		  and cw.ends_at <= ?
		  and not exists (
		    select 1 from retrospective r
		    where r.curriculum_week_id = cw.id and r.member_id = gm.id
		  )
		""";

	private final JdbcTemplate jdbcTemplate;
	private final NotificationEventPublisher publisher;
	private final Clock clock;

	RetrospectiveReminderScheduler(JdbcTemplate jdbcTemplate, NotificationEventPublisher publisher, Clock clock) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
		this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	@Scheduled(fixedDelayString = "${studypot.retrospective-reminder.interval-ms:600000}")
	void sendDueSoonReminders() {
		Instant now = clock.instant();
		Instant windowEnd = now.plus(REMINDER_WINDOW);
		List<Reminder> reminders;
		try {
			reminders = jdbcTemplate.query(
				SELECT_DUE_SOON_REMINDERS,
				(resultSet, rowNumber) -> new Reminder(
					UuidBinary.fromBytes(resultSet.getBytes("group_id")),
					UuidBinary.fromBytes(resultSet.getBytes("week_id")),
					UuidBinary.fromBytes(resultSet.getBytes("user_id"))
				),
				Timestamp.from(now),
				Timestamp.from(windowEnd)
			);
		} catch (RuntimeException exception) {
			log.warn("retrospective reminder query failed", exception);
			return;
		}
		for (Reminder reminder : reminders) {
			try {
				publisher.publishRetrospectiveReminder(reminder.groupId(), reminder.userId(), reminder.weekId());
			} catch (RuntimeException exception) {
				log.warn("retrospective reminder publish failed weekId={} userId={}", reminder.weekId(), reminder.userId(), exception);
			}
		}
	}

	private record Reminder(UUID groupId, UUID weekId, UUID userId) {
	}
}
