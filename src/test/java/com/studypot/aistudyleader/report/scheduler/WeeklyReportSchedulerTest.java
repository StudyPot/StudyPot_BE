package com.studypot.aistudyleader.report.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.studypot.aistudyleader.curriculum.service.NextWeekPlanService;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import com.studypot.aistudyleader.report.service.MemberRetrospectiveSummary;
import com.studypot.aistudyleader.report.service.StudyCompletionReportGenerator;
import com.studypot.aistudyleader.report.service.WeeklyReportGeneration;
import com.studypot.aistudyleader.report.service.WeeklyReportGenerator;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardType;
import com.studypot.aistudyleader.studygroup.board.service.CreateGroupBoardPostCommand;
import com.studypot.aistudyleader.studygroup.board.service.GroupBoardNotFoundException;
import com.studypot.aistudyleader.studygroup.board.service.GroupBoardService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class WeeklyReportSchedulerTest {

	private static final Instant NOW = Instant.parse("2026-06-25T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final WeeklyReportGenerator reportGenerator = mock(WeeklyReportGenerator.class);
	private final LlmUsageRecorder usageRecorder = mock(LlmUsageRecorder.class);
	private final GroupBoardService boardService = mock(GroupBoardService.class);

	@SuppressWarnings("unchecked")
	private final ObjectProvider<WeeklyReportGenerator> reportGeneratorProvider = mock(ObjectProvider.class);
	@SuppressWarnings("unchecked")
	private final ObjectProvider<StudyCompletionReportGenerator> completionGeneratorProvider = mock(ObjectProvider.class);
	@SuppressWarnings("unchecked")
	private final ObjectProvider<LlmUsageRecorder> usageRecorderProvider = mock(ObjectProvider.class);
	@SuppressWarnings("unchecked")
	private final ObjectProvider<NextWeekPlanService> nextWeekPlanProvider = mock(ObjectProvider.class);

	private final WeeklyReportScheduler scheduler = new WeeklyReportScheduler(
		jdbcTemplate,
		reportGeneratorProvider,
		completionGeneratorProvider,
		usageRecorderProvider,
		boardService,
		nextWeekPlanProvider,
		CLOCK
	);

	@Test
	void dueReportWeekQueryExcludesDeletedGroups() {
		// 대상 선정 단계에서 삭제된 그룹을 거르도록 study_group 조인/조건이 포함되어야 한다.
		assertThat(WeeklyReportScheduler.SELECT_DUE_REPORT_WEEKS)
			.contains("join study_group sg on sg.id = c.group_id")
			.contains("sg.deleted_at is null");
	}

	@Test
	void skipsGroupDeletedByRaceAndContinuesWithoutWarn() {
		UUID liveGroup = UUID.fromString("018f0000-0000-7000-8000-000000000b01");
		UUID deletedGroup = UUID.fromString("018f0000-0000-7000-8000-000000000b02");
		UUID weekLive = UUID.fromString("018f0000-0000-7000-8000-000000000b11");
		UUID weekDeleted = UUID.fromString("018f0000-0000-7000-8000-000000000b12");
		UUID ownerId = UUID.fromString("018f0000-0000-7000-8000-000000000b21");

		when(reportGeneratorProvider.getIfAvailable()).thenReturn(reportGenerator);
		when(usageRecorderProvider.getIfAvailable()).thenReturn(usageRecorder);
		when(completionGeneratorProvider.getIfAvailable()).thenReturn(null);
		when(nextWeekPlanProvider.getIfAvailable()).thenReturn(null);

		// 아직 리포트 없음.
		when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), any(Object[].class))).thenReturn(false);
		when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class), any(Object[].class))).thenReturn(0);
		// 회고가 있어 리포트 생성 대상이 된다.
		when(jdbcTemplate.query(any(String.class), any(RowMapper.class), any(Object[].class)))
			.thenAnswer(invocation -> {
				String sql = invocation.getArgument(0);
				if (sql.equals(WeeklyReportScheduler.SELECT_DUE_REPORT_WEEKS)) {
					return List.of(
						new WeeklyReportScheduler.DueWeek(liveGroup, weekLive, 1, "1주차"),
						new WeeklyReportScheduler.DueWeek(deletedGroup, weekDeleted, 1, "1주차")
					);
				}
				if (sql.contains("from retrospective")) {
					return List.of(new MemberRetrospectiveSummary("멤버", "{}"));
				}
				if (sql.contains("gm.permission = 'OWNER'")) {
					return List.of(ownerId);
				}
				return List.of();
			});

		// 살아있는 그룹은 정상 게시되도록, 삭제된 그룹은 게시판 조회에서 예외가 난다(레이스).
		when(boardService.findOrCreateBoardId(any(), eq(liveGroup), eq(GroupBoardType.LEADER_REPORT)))
			.thenReturn(UUID.fromString("018f0000-0000-7000-8000-000000000b31"));
		when(boardService.findOrCreateBoardId(any(), eq(deletedGroup), eq(GroupBoardType.LEADER_REPORT)))
			.thenThrow(new GroupBoardNotFoundException("study group was not found."));

		WeeklyReportGeneration generation = mock(WeeklyReportGeneration.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
		when(generation.content().title()).thenReturn("제목");
		when(generation.content().body()).thenReturn("본문");
		when(reportGenerator.generate(any())).thenReturn(generation);

		ListAppender<ILoggingEvent> appender = attachAppender();

		assertThatCode(scheduler::generateDueReports).doesNotThrowAnyException();

		// 두 그룹 모두 시도된다(삭제 그룹 실패가 살아있는 그룹 처리를 막지 않는다).
		verify(boardService).findOrCreateBoardId(any(), eq(liveGroup), eq(GroupBoardType.LEADER_REPORT));
		verify(boardService).findOrCreateBoardId(any(), eq(deletedGroup), eq(GroupBoardType.LEADER_REPORT));
		// 살아있는 그룹만 실제 게시된다.
		verify(boardService, times(1)).createPost(any(CreateGroupBoardPostCommand.class));
		// 삭제된 그룹은 WARN 노이즈 없이 DEBUG 로 스킵된다.
		assertThat(appender.list).noneMatch(event -> event.getLevel() == Level.WARN);
	}

	private ListAppender<ILoggingEvent> attachAppender() {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		ch.qos.logback.classic.Logger logger = context.getLogger(WeeklyReportScheduler.class);
		logger.setLevel(Level.DEBUG);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.setContext(context);
		appender.start();
		logger.addAppender(appender);
		return appender;
	}
}
