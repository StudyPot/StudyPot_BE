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

	// 리포트는 마감 +30분에 생성된다. 그 10분 전(마감 +20분)에 아직 회고를 안 쓴 멤버에게 마지막 리마인더를 보낸다.
	private static final Duration REPORT_DELAY = Duration.ofMinutes(30);
	private static final Duration FINAL_REMINDER_LEAD = Duration.ofMinutes(10);

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

	// 마감이 지난(리포트 생성 직전 [마감+20분, 마감+30분) 구간) 주차에서, 아직 회고를 안 썼고 리포트도 아직 안 올라온
	// 활성 멤버에게 보내는 마지막 리마인더. 1시간 전 리마인더와 멱등키가 분리돼 있어 별도로 한 번 더 발송된다.
	private static final String SELECT_FINAL_REMINDERS = """
		select c.group_id, cw.id as week_id, gm.user_id
		from curriculum_week cw
		join curriculum c on c.id = cw.curriculum_id
		join group_member gm on gm.group_id = c.group_id
		where c.status = 'ACTIVE'
		  and cw.deleted_at is null
		  and c.deleted_at is null
		  and gm.status = 'ACTIVE'
		  and gm.deleted_at is null
		  and cw.ends_at <= ?
		  and cw.ends_at > ?
		  and not exists (
		    select 1 from retrospective r
		    where r.curriculum_week_id = cw.id and r.member_id = gm.id
		  )
		  and not exists (
		    select 1 from group_board_post gbp
		    where gbp.group_id = c.group_id
		      and gbp.title = concat(cw.week_number, '주차 학습 리포트')
		      and gbp.deleted_at is null
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
		sendOneHourReminders(now);
		sendFinalReminders(now);
	}

	// 마감 1시간 전 리마인더(진행 중 주차의 미제출 멤버).
	private void sendOneHourReminders(Instant now) {
		Instant windowEnd = now.plus(REMINDER_WINDOW);
		List<Reminder> reminders = query(
			SELECT_DUE_SOON_REMINDERS,
			Timestamp.from(now),
			Timestamp.from(windowEnd)
		);
		for (Reminder reminder : reminders) {
			try {
				publisher.publishRetrospectiveReminder(reminder.groupId(), reminder.userId(), reminder.weekId());
			} catch (RuntimeException exception) {
				log.warn("retrospective reminder publish failed weekId={} userId={}", reminder.weekId(), reminder.userId(), exception);
			}
		}
	}

	// 리포트 생성 10분 전(마감 +20분 ~ +30분) 마지막 리마인더(미제출·리포트 미게시 멤버).
	private void sendFinalReminders(Instant now) {
		Instant windowOpensBefore = now.minus(REPORT_DELAY.minus(FINAL_REMINDER_LEAD)); // ends_at <= now - 20분
		Instant windowClosesBefore = now.minus(REPORT_DELAY); // ends_at > now - 30분
		List<Reminder> reminders = query(
			SELECT_FINAL_REMINDERS,
			Timestamp.from(windowOpensBefore),
			Timestamp.from(windowClosesBefore)
		);
		for (Reminder reminder : reminders) {
			try {
				publisher.publishRetrospectiveFinalReminder(reminder.groupId(), reminder.userId(), reminder.weekId());
			} catch (RuntimeException exception) {
				log.warn("retrospective final reminder publish failed weekId={} userId={}", reminder.weekId(), reminder.userId(), exception);
			}
		}
	}

	private List<Reminder> query(String sql, Timestamp first, Timestamp second) {
		try {
			return jdbcTemplate.query(
				sql,
				(resultSet, rowNumber) -> new Reminder(
					UuidBinary.fromBytes(resultSet.getBytes("group_id")),
					UuidBinary.fromBytes(resultSet.getBytes("week_id")),
					UuidBinary.fromBytes(resultSet.getBytes("user_id"))
				),
				first,
				second
			);
		} catch (RuntimeException exception) {
			log.warn("retrospective reminder query failed", exception);
			return List.of();
		}
	}

	private record Reminder(UUID groupId, UUID weekId, UUID userId) {
	}
}
