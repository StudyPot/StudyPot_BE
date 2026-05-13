package com.studypot.aistudyleader.retrospective.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.retrospective.domain.Retrospective;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveMembershipContext;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveStatus;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveTriggerType;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcRetrospectiveRepositoryTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000006301");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000006302");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000006303");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000006304");
	private static final UUID PROGRESS_ID = UUID.fromString("018f0000-0000-7000-8000-000000006305");
	private static final UUID RETROSPECTIVE_ID = UUID.fromString("018f0000-0000-7000-8000-000000006306");
	private static final UUID LLM_USAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000006307");
	private static final Instant NOW = Instant.parse("2026-05-12T03:15:00Z");

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final JdbcRetrospectiveRepository repository = new JdbcRetrospectiveRepository(jdbcTemplate, new ObjectMapper());

	@Test
	void membershipSqlFindsMemberThroughWeekGroupAndFiltersSoftDeletes() {
		assertThat(RetrospectiveJdbcSql.SELECT_WEEK_MEMBERSHIP)
			.contains("from curriculum_week cw")
			.contains("join curriculum c on c.id = cw.curriculum_id")
			.contains("join study_group sg on sg.id = c.group_id")
			.contains("join group_member gm on gm.group_id = sg.id")
			.contains("cw.id = ?")
			.contains("gm.user_id = ?")
			.contains("gm.deleted_at is null");
	}

	@Test
	void retrospectiveSqlUsesProgressWeekMemberKeyAndTaskSummaryLeftJoin() {
		assertThat(RetrospectiveJdbcSql.SELECT_RETROSPECTIVE)
			.contains("from retrospective")
			.contains("where progress_id = ?")
			.contains("and curriculum_week_id = ?")
			.contains("and member_id = ?");
		assertThat(RetrospectiveJdbcSql.SELECT_TASK_SUMMARIES)
			.contains("from weekly_task wt")
			.contains("left join task_completion tc")
			.contains("coalesce(tc.status, 'TODO')")
			.contains("wt.curriculum_week_id = ?")
			.contains("order by wt.display_order");
		assertThat(RetrospectiveJdbcSql.SELECT_RETROSPECTIVE_BY_ID)
			.contains("from retrospective")
			.contains("where id = ?");
		assertThat(RetrospectiveJdbcSql.UPDATE_RETROSPECTIVE_RESULT)
			.contains("update retrospective")
			.contains("ai_feedback = ?")
			.contains("next_week_adjustment = ?")
			.contains("status = ?");
		assertThat(RetrospectiveJdbcSql.SELECT_ONBOARDING_SUMMARY)
			.contains("from group_onboarding_response")
			.contains("where group_id = ?")
			.contains("and member_id = ?");
		assertThat(RetrospectiveJdbcSql.SELECT_ACTIVE_RULE_SUMMARIES)
			.contains("from group_rule")
			.contains("where group_id = ?")
			.contains("and is_active = 1");
		assertThat(RetrospectiveJdbcSql.SELECT_RULE_VIOLATION_SUMMARIES)
			.contains("from rule_violation rv")
			.contains("join group_rule gr on gr.id = rv.rule_id")
			.contains("gr.group_id = ?")
			.contains("rv.member_id = ?");
		assertThat(RetrospectiveJdbcSql.SELECT_PRIOR_RETROSPECTIVES)
			.contains("from retrospective")
			.contains("where member_id = ?")
			.contains("and id <> ?");
		assertThat(RetrospectiveJdbcSql.SELECT_RETROSPECTIVE_CONVERSATION_SUMMARY)
			.contains("from ai_conversation")
			.contains("where retrospective_id = ?")
			.contains("and member_id = ?")
			.doesNotContain("ai_conversation_message");
	}

	@Test
	void findMembershipByWeekIdQueriesWeekAndUser() {
		RetrospectiveMembershipContext context = new RetrospectiveMembershipContext(
			GROUP_ID,
			MEMBER_ID,
			StudyGroupStatus.ACTIVE,
			GroupMemberPermission.MEMBER,
			GroupMemberStatus.ACTIVE
		);
		when(jdbcTemplate.query(eq(RetrospectiveJdbcSql.SELECT_WEEK_MEMBERSHIP), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(context));

		Optional<RetrospectiveMembershipContext> result = repository.findMembershipByWeekId(WEEK_ID, USER_ID);

		assertThat(result).contains(context);
		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(RetrospectiveJdbcSql.SELECT_WEEK_MEMBERSHIP), any(org.springframework.jdbc.core.RowMapper.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(WEEK_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(USER_ID));
	}

	@Test
	void insertRetrospectivePersistsJsonStatusAndTimestamps() {
		Retrospective retrospective = new Retrospective(
			RETROSPECTIVE_ID,
			PROGRESS_ID,
			WEEK_ID,
			MEMBER_ID,
			null,
			RetrospectiveTriggerType.MANUAL,
			Map.of("progress", Map.of("status", "COMPLETED")),
			Map.of(),
			Map.of(),
			RetrospectiveStatus.PENDING,
			NOW,
			null,
			NOW,
			NOW
		);
		when(jdbcTemplate.update(eq(RetrospectiveJdbcSql.INSERT_RETROSPECTIVE), any(Object[].class))).thenReturn(1);

		assertThat(repository.insertRetrospective(retrospective)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(RetrospectiveJdbcSql.INSERT_RETROSPECTIVE), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(RETROSPECTIVE_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(PROGRESS_ID));
		assertThat((byte[]) args.getValue()[2]).containsExactly(UuidBinary.toBytes(WEEK_ID));
		assertThat((byte[]) args.getValue()[3]).containsExactly(UuidBinary.toBytes(MEMBER_ID));
		assertThat(args.getValue()[5]).isEqualTo("MANUAL");
		assertThat(args.getValue()[6].toString()).contains("\"progress\"");
		assertThat(args.getValue()[9]).isEqualTo("PENDING");
		assertThat(args.getValue()[10]).isEqualTo(Timestamp.from(NOW));
		assertThat(args.getValue()[11]).isNull();
	}

	@Test
	void findRetrospectiveByIdQueriesPrimaryKey() {
		Retrospective retrospective = completedRetrospective();
		when(jdbcTemplate.query(eq(RetrospectiveJdbcSql.SELECT_RETROSPECTIVE_BY_ID), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(retrospective));

		Optional<Retrospective> result = repository.findRetrospectiveById(RETROSPECTIVE_ID);

		assertThat(result).contains(retrospective);
		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(RetrospectiveJdbcSql.SELECT_RETROSPECTIVE_BY_ID), any(org.springframework.jdbc.core.RowMapper.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(RETROSPECTIVE_ID));
	}

	@Test
	void findAiContextQueriesOwnMemberContextSources() {
		when(jdbcTemplate.query(any(String.class), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of());

		assertThat(repository.findAiContext(GROUP_ID, MEMBER_ID, WEEK_ID, RETROSPECTIVE_ID).ruleViolations()).isEmpty();

		ArgumentCaptor<Object[]> onboardingArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(
			eq(RetrospectiveJdbcSql.SELECT_ONBOARDING_SUMMARY),
			any(org.springframework.jdbc.core.RowMapper.class),
			onboardingArgs.capture()
		);
		assertThat((byte[]) onboardingArgs.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) onboardingArgs.getValue()[1]).containsExactly(UuidBinary.toBytes(MEMBER_ID));
		ArgumentCaptor<Object[]> violationArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(
			eq(RetrospectiveJdbcSql.SELECT_RULE_VIOLATION_SUMMARIES),
			any(org.springframework.jdbc.core.RowMapper.class),
			violationArgs.capture()
		);
		assertThat((byte[]) violationArgs.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) violationArgs.getValue()[1]).containsExactly(UuidBinary.toBytes(MEMBER_ID));
		ArgumentCaptor<Object[]> priorArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(
			eq(RetrospectiveJdbcSql.SELECT_PRIOR_RETROSPECTIVES),
			any(org.springframework.jdbc.core.RowMapper.class),
			priorArgs.capture()
		);
		assertThat((byte[]) priorArgs.getValue()[0]).containsExactly(UuidBinary.toBytes(MEMBER_ID));
		assertThat((byte[]) priorArgs.getValue()[1]).containsExactly(UuidBinary.toBytes(RETROSPECTIVE_ID));
		assertThat((byte[]) priorArgs.getValue()[2]).containsExactly(UuidBinary.toBytes(WEEK_ID));
		ArgumentCaptor<Object[]> conversationArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(
			eq(RetrospectiveJdbcSql.SELECT_RETROSPECTIVE_CONVERSATION_SUMMARY),
			any(org.springframework.jdbc.core.RowMapper.class),
			conversationArgs.capture()
		);
		assertThat((byte[]) conversationArgs.getValue()[0]).containsExactly(UuidBinary.toBytes(RETROSPECTIVE_ID));
		assertThat((byte[]) conversationArgs.getValue()[1]).containsExactly(UuidBinary.toBytes(MEMBER_ID));
	}

	@Test
	void updateRetrospectiveResultPersistsFeedbackAdjustmentAndStatus() {
		Retrospective retrospective = completedRetrospective();
		when(jdbcTemplate.update(eq(RetrospectiveJdbcSql.UPDATE_RETROSPECTIVE_RESULT), any(Object[].class))).thenReturn(1);

		assertThat(repository.updateRetrospectiveResult(retrospective)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(RetrospectiveJdbcSql.UPDATE_RETROSPECTIVE_RESULT), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(LLM_USAGE_ID));
		assertThat(args.getValue()[1].toString()).contains("\"summary\"");
		assertThat(args.getValue()[2].toString()).contains("\"difficulty\"");
		assertThat(args.getValue()[3]).isEqualTo("COMPLETED");
		assertThat(args.getValue()[4]).isEqualTo(Timestamp.from(NOW));
		assertThat(args.getValue()[5]).isEqualTo(Timestamp.from(NOW));
		assertThat((byte[]) args.getValue()[6]).containsExactly(UuidBinary.toBytes(RETROSPECTIVE_ID));
	}

	private static Retrospective completedRetrospective() {
		return new Retrospective(
			RETROSPECTIVE_ID,
			PROGRESS_ID,
			WEEK_ID,
			MEMBER_ID,
			LLM_USAGE_ID,
			RetrospectiveTriggerType.MANUAL,
			Map.of("progress", Map.of("status", "COMPLETED")),
			Map.of("summary", "이번 주 학습 흐름이 좋습니다."),
			Map.of("difficulty", "slightly_lower"),
			RetrospectiveStatus.COMPLETED,
			NOW.minusSeconds(120),
			NOW,
			NOW.minusSeconds(120),
			NOW
		);
	}
}
