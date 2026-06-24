package com.studypot.aistudyleader.report.scheduler;

import com.studypot.aistudyleader.global.domain.UuidV7;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import com.studypot.aistudyleader.curriculum.service.NextWeekPlanService;
import com.studypot.aistudyleader.report.service.MemberRetrospectiveSummary;
import com.studypot.aistudyleader.report.service.MemberTaskProgress;
import com.studypot.aistudyleader.report.service.WeeklyReportData;
import com.studypot.aistudyleader.report.service.WeeklyReportGeneration;
import com.studypot.aistudyleader.report.service.WeeklyReportGenerator;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardType;
import com.studypot.aistudyleader.studygroup.board.service.CreateGroupBoardPostCommand;
import com.studypot.aistudyleader.studygroup.board.service.GroupBoardService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 주차 마감(ends_at 경과) 시, 해당 주차 모든 멤버의 회고를 종합해 AI 주차 리포트를 만들고
 * 그룹 회고 게시판에 자동으로 글을 올린다. 같은 주차에 리포트가 이미 있으면(제목 기준) 건너뛴다.
 */
@Component
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
class WeeklyReportScheduler {

	private static final Logger log = LoggerFactory.getLogger(WeeklyReportScheduler.class);

	// 스케줄러가 잠시 멈췄어도 놓치지 않도록 마감 후 일정 기간(기본 7일) 내 주차를 대상으로 한다.
	// 중복 작성은 게시글 제목으로 멱등 처리한다.
	private static final Duration LOOKBACK = Duration.ofDays(7);

	private static final String SELECT_DUE_REPORT_WEEKS = """
		select c.group_id, cw.id as week_id, cw.week_number, cw.title as week_title
		from curriculum_week cw
		join curriculum c on c.id = cw.curriculum_id
		where c.status = 'ACTIVE'
		  and c.deleted_at is null
		  and cw.deleted_at is null
		  and cw.ends_at <= ?
		  and cw.ends_at > ?
		order by cw.ends_at
		""";

	private static final String SELECT_OWNER_USER_ID = """
		select gm.user_id
		from group_member gm
		where gm.group_id = ?
		  and gm.permission = 'OWNER'
		  and gm.status <> 'LEFT'
		  and gm.deleted_at is null
		limit 1
		""";

	private static final String SELECT_MEMBER_RETROSPECTIVES = """
		select coalesce(nullif(gm.display_name, ''), u.nickname) as member_name,
		       json_extract(r.input_summary, '$.answers') as feedback_summary
		from retrospective r
		join group_member gm on gm.id = r.member_id
		join users u on u.id = gm.user_id
		where r.curriculum_week_id = ?
		  and r.status = 'COMPLETED'
		order by r.requested_at, r.id
		""";

	private static final String SELECT_MEMBER_TASK_PROGRESS = """
		select coalesce(nullif(gm.display_name, ''), u.nickname) as member_name,
		       coalesce(sum(case when tc.status = 'DONE' then 1 else 0 end), 0) as done_count
		from group_member gm
		join users u on u.id = gm.user_id
		left join task_completion tc on tc.member_id = gm.id
		  and tc.weekly_task_id in (
		    select wt.id from weekly_task wt where wt.curriculum_week_id = ? and wt.deleted_at is null
		  )
		where gm.group_id = ?
		  and gm.status = 'ACTIVE'
		  and gm.deleted_at is null
		group by gm.id, member_name
		order by member_name
		""";

	private static final String COUNT_WEEK_TASKS = """
		select count(*) from weekly_task where curriculum_week_id = ? and deleted_at is null
		""";

	private static final String EXISTS_REPORT_POST = """
		select exists (
		  select 1
		  from group_board_post
		  where group_id = ?
		    and title = ?
		    and deleted_at is null
		)
		""";

	private final JdbcTemplate jdbcTemplate;
	private final ObjectProvider<WeeklyReportGenerator> reportGenerator;
	private final ObjectProvider<LlmUsageRecorder> usageRecorder;
	private final GroupBoardService boardService;
	private final ObjectProvider<NextWeekPlanService> nextWeekPlanService;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;

	WeeklyReportScheduler(
		JdbcTemplate jdbcTemplate,
		ObjectProvider<WeeklyReportGenerator> reportGenerator,
		ObjectProvider<LlmUsageRecorder> usageRecorder,
		GroupBoardService boardService,
		ObjectProvider<NextWeekPlanService> nextWeekPlanService,
		Clock clock
	) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
		this.reportGenerator = Objects.requireNonNull(reportGenerator, "reportGenerator must not be null");
		this.usageRecorder = Objects.requireNonNull(usageRecorder, "usageRecorder must not be null");
		this.boardService = Objects.requireNonNull(boardService, "boardService must not be null");
		this.nextWeekPlanService = Objects.requireNonNull(nextWeekPlanService, "nextWeekPlanService must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = UuidV7::generate;
	}

	@Scheduled(fixedDelayString = "${studypot.weekly-report.interval-ms:900000}")
	void generateDueReports() {
		WeeklyReportGenerator generator = reportGenerator.getIfAvailable();
		LlmUsageRecorder recorder = usageRecorder.getIfAvailable();
		if (generator == null || recorder == null) {
			return; // AI 미구성 환경에서는 동작하지 않는다.
		}
		Instant now = clock.instant();
		List<DueWeek> weeks;
		try {
			weeks = jdbcTemplate.query(
				SELECT_DUE_REPORT_WEEKS,
				(rs, rowNum) -> new DueWeek(
					UuidBinary.fromBytes(rs.getBytes("group_id")),
					UuidBinary.fromBytes(rs.getBytes("week_id")),
					rs.getInt("week_number"),
					rs.getString("week_title")
				),
				Timestamp.from(now),
				Timestamp.from(now.minus(LOOKBACK))
			);
		} catch (RuntimeException exception) {
			log.warn("weekly report due-week query failed", exception);
			return;
		}
		for (DueWeek week : weeks) {
			try {
				generateForWeek(week, generator, recorder);
			} catch (RuntimeException exception) {
				log.warn("weekly report generation failed groupId={} weekId={}", week.groupId(), week.weekId(), exception);
			}
		}
	}

	private void generateForWeek(DueWeek week, WeeklyReportGenerator generator, LlmUsageRecorder recorder) {
		String title = week.weekNumber() + "주차 학습 리포트";
		Boolean exists = jdbcTemplate.queryForObject(EXISTS_REPORT_POST, Boolean.class, UuidBinary.toBytes(week.groupId()), title);
		if (Boolean.TRUE.equals(exists)) {
			return; // 이미 리포트가 작성된 주차
		}
		List<MemberRetrospectiveSummary> retros = jdbcTemplate.query(
			SELECT_MEMBER_RETROSPECTIVES,
			(rs, rowNum) -> new MemberRetrospectiveSummary(rs.getString("member_name"), rs.getString("feedback_summary")),
			UuidBinary.toBytes(week.weekId())
		);
		List<MemberTaskProgress> taskProgress = findMemberTaskProgress(week);
		if (retros.isEmpty() && taskProgress.isEmpty()) {
			return; // 회고도 멤버도 없으면 리포트를 만들 수 없다.
		}
		Optional<UUID> ownerUserId = findOwnerUserId(week.groupId());
		if (ownerUserId.isEmpty()) {
			return;
		}
		UUID leaderReportBoardId = boardService.findOrCreateBoardId(ownerUserId.get(), week.groupId(), GroupBoardType.LEADER_REPORT);
		WeeklyReportGeneration generation = generator.generate(
			new WeeklyReportData(week.groupId(), week.weekId(), week.weekNumber(), week.weekTitle(), retros, taskProgress)
		);
		recorder.record(generation.response().toUsage(
			idGenerator.get(),
			ownerUserId.get(),
			week.groupId(),
			LlmUsagePurpose.WEEKLY_REPORT,
			clock.instant()
		));
		// 멱등 체크(EXISTS_REPORT_POST)와 동일한 결정적 제목으로 글을 작성해야 다음 실행에서 중복 생성이 막힌다.
		// LLM 이 생성한 제목은 본문 머리말(H1)로 보존한다.
		String body = "# " + generation.content().title() + "\n\n" + generation.content().body();
		boardService.createPost(new CreateGroupBoardPostCommand(
			ownerUserId.get(),
			week.groupId(),
			leaderReportBoardId,
			title,
			body,
			false
		));
		log.info("weekly report posted groupId={} weekNumber={}", week.groupId(), week.weekNumber());
		regenerateNextWeek(week, ownerUserId.get());
	}

	// 리포트가 올라온 직후, 그 리포트+직전 주차 TODO+멤버 회고를 바탕으로 '다음 주차'를 점진 생성(신규 삽입)한다.
	// 리포트 게시가 멱등(주차당 1회)이고 다음 주차 생성도 멱등이라 주차당 1회만 일어난다. 실패해도 리포트 게시는 유지한다.
	private void regenerateNextWeek(DueWeek week, UUID ownerUserId) {
		NextWeekPlanService service = nextWeekPlanService.getIfAvailable();
		if (service == null) {
			return;
		}
		try {
			service.createNextWeekAutomatically(week.groupId(), week.weekId(), ownerUserId);
		} catch (RuntimeException exception) {
			log.warn("next week auto-creation failed groupId={} weekId={}", week.groupId(), week.weekId(), exception);
		}
	}

	private List<MemberTaskProgress> findMemberTaskProgress(DueWeek week) {
		int totalTasks = totalWeekTasks(week.weekId());
		return jdbcTemplate.query(
			SELECT_MEMBER_TASK_PROGRESS,
			(rs, rowNum) -> new MemberTaskProgress(rs.getString("member_name"), rs.getInt("done_count"), totalTasks),
			UuidBinary.toBytes(week.weekId()),
			UuidBinary.toBytes(week.groupId())
		);
	}

	private int totalWeekTasks(UUID weekId) {
		Integer count = jdbcTemplate.queryForObject(COUNT_WEEK_TASKS, Integer.class, UuidBinary.toBytes(weekId));
		return count == null ? 0 : count;
	}

	private Optional<UUID> findOwnerUserId(UUID groupId) {
		List<UUID> owners = jdbcTemplate.query(
			SELECT_OWNER_USER_ID,
			(rs, rowNum) -> UuidBinary.fromBytes(rs.getBytes("user_id")),
			UuidBinary.toBytes(groupId)
		);
		return owners.isEmpty() ? Optional.empty() : Optional.of(owners.get(0));
	}

	private record DueWeek(UUID groupId, UUID weekId, int weekNumber, String weekTitle) {
	}
}
