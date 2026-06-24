package com.studypot.aistudyleader.curriculum.scheduler;

import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.notification.service.NotificationEventPublisher;
import java.sql.Timestamp;
import java.time.Clock;
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
 * 커리큘럼 주차의 상태를 시간(starts_at/ends_at) 기준으로 자동 전이시킨다.
 * - 마감(ends_at)이 지난 주차는 COMPLETED 로 마감한다.
 * - 시작(starts_at)이 도래했고 아직 마감되지 않은 PENDING 주차는 IN_PROGRESS 로 활성화하고
 *   주차 시작 알림(WEEK_STARTED)을 발송한다(알림 멱등키로 중복 방지).
 *
 * <p>주차 상태를 바꾸는 별도 로직이 없으면 1주차만 IN_PROGRESS 로 남아 '현재 주차' 조회와
 * 회고 리마인더가 1주차에 멈추므로, 이 스케줄러가 그 전이를 담당한다.
 */
@Component
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
class WeekLifecycleScheduler {

	private static final Logger log = LoggerFactory.getLogger(WeekLifecycleScheduler.class);

	/** 마감이 지난(아직 COMPLETED 가 아닌) 활성 커리큘럼의 주차를 COMPLETED 로 전이. */
	static final String COMPLETE_ENDED_WEEKS = """
		update curriculum_week
		set status = 'COMPLETED',
		    updated_at = ?
		where status <> 'COMPLETED'
		  and ends_at <= ?
		  and deleted_at is null
		  and curriculum_id in (
		    select id from curriculum where status = 'ACTIVE' and deleted_at is null
		  )
		""";

	/**
	 * COMPLETED 주차의 아직 끝내지 못한(TODO) 과제 완료를 INCOMPLETE 로 확정한다.
	 * 매 틱 실행해도 TODO 행만 대상이라 멱등이다. (DONE/SKIPPED/이미 INCOMPLETE 는 건드리지 않는다)
	 */
	static final String MARK_INCOMPLETE_TODOS = """
		update task_completion
		set status = 'INCOMPLETE',
		    updated_at = ?
		where status = 'TODO'
		  and weekly_task_id in (
		    select wt.id
		    from weekly_task wt
		    join curriculum_week cw on cw.id = wt.curriculum_week_id
		    join curriculum c on c.id = cw.curriculum_id
		    where cw.status = 'COMPLETED'
		      and cw.deleted_at is null
		      and wt.deleted_at is null
		      and c.status = 'ACTIVE'
		      and c.deleted_at is null
		  )
		""";

	/** 시작이 도래했고 아직 진행 중이어야 하는 PENDING 주차를 활성화 대상으로 조회. */
	static final String SELECT_WEEKS_TO_ACTIVATE = """
		select c.group_id, cw.id as week_id, cw.week_number, cw.title
		from curriculum_week cw
		join curriculum c on c.id = cw.curriculum_id
		where cw.status = 'PENDING'
		  and cw.starts_at <= ?
		  and cw.ends_at > ?
		  and cw.deleted_at is null
		  and c.status = 'ACTIVE'
		  and c.deleted_at is null
		""";

	/** 조회된 PENDING 주차를 IN_PROGRESS 로 전이(동시성 대비 PENDING 조건 유지). */
	static final String ACTIVATE_WEEK = """
		update curriculum_week
		set status = 'IN_PROGRESS',
		    updated_at = ?
		where id = ?
		  and status = 'PENDING'
		""";

	/**
	 * 모든 계획 주차(total_weeks)가 생성·COMPLETED 된 ACTIVE 그룹을 '스터디 완료' 대상으로 조회.
	 * (마지막 주차까지 도달했고 미완료 주차가 하나도 없을 때)
	 */
	static final String SELECT_FINISHED_STUDIES = """
		select sg.id as group_id, sg.name as group_name
		from study_group sg
		join curriculum c on c.group_id = sg.id and c.status = 'ACTIVE' and c.deleted_at is null
		where sg.status = 'ACTIVE'
		  and sg.deleted_at is null
		  and (
		    select max(cw.week_number)
		    from curriculum_week cw
		    where cw.curriculum_id = c.id and cw.deleted_at is null
		  ) >= c.total_weeks
		  and not exists (
		    select 1 from curriculum_week cw2
		    where cw2.curriculum_id = c.id and cw2.deleted_at is null and cw2.status <> 'COMPLETED'
		  )
		""";

	/** 그룹을 COMPLETED 로 전이(동시성 대비 ACTIVE 조건 유지, 멱등). */
	static final String COMPLETE_STUDY_GROUP = """
		update study_group
		set status = 'COMPLETED',
		    updated_at = ?
		where id = ?
		  and status = 'ACTIVE'
		""";

	/** 그룹의 활성 커리큘럼도 COMPLETED 로 전이. */
	static final String COMPLETE_CURRICULUM = """
		update curriculum
		set status = 'COMPLETED',
		    updated_at = ?
		where group_id = ?
		  and status = 'ACTIVE'
		""";

	private final JdbcTemplate jdbcTemplate;
	private final NotificationEventPublisher publisher;
	private final Clock clock;

	WeekLifecycleScheduler(JdbcTemplate jdbcTemplate, NotificationEventPublisher publisher, Clock clock) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
		this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	@Scheduled(fixedDelayString = "${studypot.week-lifecycle.interval-ms:300000}")
	void advanceWeeks() {
		Instant now = clock.instant();
		completeEndedWeeks(now);
		activateStartedWeeks(now);
		completeFinishedStudies(now);
	}

	/** 모든 계획 주차가 끝난 그룹을 COMPLETED 로 전이하고 전원에게 완료 알림을 보낸다. */
	private void completeFinishedStudies(Instant now) {
		List<FinishedStudy> finished;
		try {
			finished = jdbcTemplate.query(
				SELECT_FINISHED_STUDIES,
				(resultSet, rowNumber) -> new FinishedStudy(
					UuidBinary.fromBytes(resultSet.getBytes("group_id")),
					resultSet.getString("group_name")
				)
			);
		} catch (RuntimeException exception) {
			log.warn("selecting finished studies failed", exception);
			return;
		}
		for (FinishedStudy study : finished) {
			completeStudy(study, now);
		}
	}

	private void completeStudy(FinishedStudy study, Instant now) {
		int updated;
		try {
			updated = jdbcTemplate.update(COMPLETE_STUDY_GROUP, Timestamp.from(now), UuidBinary.toBytes(study.groupId()));
		} catch (RuntimeException exception) {
			log.warn("completing study group failed groupId={}", study.groupId(), exception);
			return;
		}
		if (updated == 0) {
			return; // 다른 틱에서 이미 완료 처리됨(멱등)
		}
		try {
			jdbcTemplate.update(COMPLETE_CURRICULUM, Timestamp.from(now), UuidBinary.toBytes(study.groupId()));
		} catch (RuntimeException exception) {
			log.warn("completing curriculum failed groupId={}", study.groupId(), exception);
		}
		try {
			publisher.publishStudyCompleted(study.groupId(), study.groupName());
		} catch (RuntimeException exception) {
			log.warn("study completed notification failed groupId={}", study.groupId(), exception);
		}
		log.info("completed study groupId={}", study.groupId());
	}

	private void completeEndedWeeks(Instant now) {
		try {
			int completed = jdbcTemplate.update(COMPLETE_ENDED_WEEKS, Timestamp.from(now), Timestamp.from(now));
			if (completed > 0) {
				log.info("completed {} ended curriculum week(s)", completed);
			}
		} catch (RuntimeException exception) {
			log.warn("completing ended weeks failed", exception);
		}
		try {
			int marked = jdbcTemplate.update(MARK_INCOMPLETE_TODOS, Timestamp.from(now));
			if (marked > 0) {
				log.info("marked {} task completion(s) as INCOMPLETE for ended week(s)", marked);
			}
		} catch (RuntimeException exception) {
			log.warn("marking incomplete task completions failed", exception);
		}
	}

	private void activateStartedWeeks(Instant now) {
		List<Activation> activations;
		try {
			activations = jdbcTemplate.query(
				SELECT_WEEKS_TO_ACTIVATE,
				(resultSet, rowNumber) -> new Activation(
					UuidBinary.fromBytes(resultSet.getBytes("group_id")),
					UuidBinary.fromBytes(resultSet.getBytes("week_id")),
					resultSet.getInt("week_number"),
					resultSet.getString("title")
				),
				Timestamp.from(now),
				Timestamp.from(now)
			);
		} catch (RuntimeException exception) {
			log.warn("selecting weeks to activate failed", exception);
			return;
		}
		for (Activation activation : activations) {
			activate(activation, now);
		}
	}

	private void activate(Activation activation, Instant now) {
		int updated;
		try {
			updated = jdbcTemplate.update(ACTIVATE_WEEK, Timestamp.from(now), UuidBinary.toBytes(activation.weekId()));
		} catch (RuntimeException exception) {
			log.warn("activating week failed weekId={}", activation.weekId(), exception);
			return;
		}
		if (updated == 0) {
			return;
		}
		try {
			publisher.publishWeekStarted(activation.groupId(), activation.weekId(), activation.weekNumber(), activation.title());
		} catch (RuntimeException exception) {
			log.warn("week started notification failed weekId={}", activation.weekId(), exception);
		}
	}

	record Activation(UUID groupId, UUID weekId, int weekNumber, String title) {
	}

	record FinishedStudy(UUID groupId, String groupName) {
	}
}
