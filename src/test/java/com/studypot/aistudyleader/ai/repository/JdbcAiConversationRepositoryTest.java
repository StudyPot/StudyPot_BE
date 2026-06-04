package com.studypot.aistudyleader.ai.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.ai.domain.AiConversation;
import com.studypot.aistudyleader.ai.domain.AiConversationStatus;
import com.studypot.aistudyleader.ai.domain.AiConversationType;
import com.studypot.aistudyleader.ai.domain.AiConversationMembershipContext;
import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageContext;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageCursor;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageSenderType;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcAiConversationRepositoryTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000009201");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000009202");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000009203");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000009204");
	private static final UUID CURRENT_WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000009214");
	private static final UUID RETROSPECTIVE_ID = UUID.fromString("018f0000-0000-7000-8000-000000009205");
	private static final UUID CONVERSATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000009206");
	private static final UUID MESSAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000009207");
	private static final UUID LLM_USAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000009208");
	private static final Instant NOW = Instant.parse("2026-05-13T01:00:00Z");

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final JdbcAiConversationRepository repository = new JdbcAiConversationRepository(jdbcTemplate, new ObjectMapper());

	@Test
	void membershipSqlFindsMemberThroughGroupAndFiltersSoftDeletes() {
		assertThat(AiConversationJdbcSql.SELECT_MEMBERSHIP)
			.contains("from study_group sg")
			.contains("join group_member gm on gm.group_id = sg.id")
			.contains("sg.id = ?")
			.contains("gm.user_id = ?")
			.contains("sg.deleted_at is null")
			.contains("gm.deleted_at is null");
	}

	@Test
	void referenceSqlScopesWeekAndRetrospectiveThroughCurriculumGroup() {
		assertThat(AiConversationJdbcSql.SELECT_WEEK_GROUP_ID)
			.contains("from curriculum_week cw")
			.contains("join curriculum c on c.id = cw.curriculum_id")
			.contains("cw.id = ?");
		assertThat(AiConversationJdbcSql.SELECT_RETROSPECTIVE_REFERENCE)
			.contains("from retrospective r")
			.contains("join curriculum_week cw on cw.id = r.curriculum_week_id")
			.contains("join curriculum c on c.id = cw.curriculum_id")
			.contains("join group_member gm on gm.id = r.member_id")
			.contains("gm.group_id = c.group_id")
			.contains("r.id = ?");
	}

	@Test
	void messageSqlFindsConversationMemberAndOrdersByCursor() {
		assertThat(AiConversationJdbcSql.EXISTS_CONVERSATION)
			.contains("from ai_conversation")
			.contains("where id = ?");
		assertThat(AiConversationJdbcSql.SELECT_MESSAGE_CONTEXT)
			.contains("from ai_conversation ac")
			.contains("ac.curriculum_week_id")
			.contains("ac.retrospective_id")
			.contains("ac.conversation_type")
			.contains("coalesce(ac.summary, '') as summary")
			.contains("join study_group sg on sg.id = ac.group_id")
			.contains("join group_member gm on gm.id = ac.member_id")
			.contains("gm.group_id = ac.group_id")
			.contains("ac.id = ?")
			.contains("gm.user_id = ?")
			.contains("sg.deleted_at is null")
			.contains("gm.deleted_at is null");
		assertThat(AiConversationJdbcSql.SELECT_MESSAGES)
			.contains("from ai_conversation_message m")
			.contains("m.conversation_id = ?")
			.contains("order by m.created_at asc, m.id asc")
			.contains("limit ?");
		assertThat(AiConversationJdbcSql.SELECT_RECENT_MESSAGES_FOR_PROMPT)
			.contains("from ai_conversation_message m")
			.contains("order by m.created_at desc, m.id desc")
			.contains("limit ?");
		assertThat(AiConversationJdbcSql.SELECT_TASK_PROMPT_CONTEXT)
			.contains("left join task_completion tc on tc.weekly_task_id = wt.id")
			.contains("tc.member_id = ?")
			.contains("tc.completion_note")
			.contains("tc.incomplete_reason");
		assertThat(AiConversationJdbcSql.SELECT_PROGRESS_PROMPT_CONTEXT)
			.contains("from member_week_progress")
			.contains("completion_note")
			.contains("incomplete_reason");
		assertThat(AiConversationJdbcSql.SELECT_RETROSPECTIVE_PROMPT_CONTEXT)
			.contains("ai_feedback")
			.contains("next_week_adjustment")
			.doesNotContain("input_summary");
	}

	@Test
	void openTeamLeadConversationSqlFindsLatestOpenOrdinaryTeamLeadConversation() {
		assertThat(AiConversationJdbcSql.SELECT_OPEN_TEAM_LEAD_CONVERSATION)
			.contains("from ai_conversation")
			.contains("group_id = ?")
			.contains("member_id = ?")
			.contains("conversation_type = 'TEAM_LEAD_CHAT'")
			.contains("status = 'OPEN'")
			.contains("curriculum_week_id is null")
			.contains("retrospective_id is null")
			.contains("order by opened_at desc, id desc")
			.contains("limit 1");
	}

	@Test
	void findMembershipQueriesGroupAndUser() {
		AiConversationMembershipContext context = new AiConversationMembershipContext(
			GROUP_ID,
			MEMBER_ID,
			StudyGroupStatus.ACTIVE,
			GroupMemberPermission.MEMBER,
			GroupMemberStatus.ACTIVE
		);
		when(jdbcTemplate.query(eq(AiConversationJdbcSql.SELECT_MEMBERSHIP), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(context));

		Optional<AiConversationMembershipContext> result = repository.findMembership(GROUP_ID, USER_ID);

		assertThat(result).contains(context);
		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(AiConversationJdbcSql.SELECT_MEMBERSHIP), any(org.springframework.jdbc.core.RowMapper.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(USER_ID));
	}

	@Test
	void findOpenTeamLeadConversationQueriesGroupAndMember() {
		AiConversation conversation = teamLeadConversation(null, null);
		when(jdbcTemplate.query(eq(AiConversationJdbcSql.SELECT_OPEN_TEAM_LEAD_CONVERSATION), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(conversation));

		Optional<AiConversation> result = repository.findOpenTeamLeadConversation(GROUP_ID, MEMBER_ID);

		assertThat(result).contains(conversation);
		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(AiConversationJdbcSql.SELECT_OPEN_TEAM_LEAD_CONVERSATION), any(org.springframework.jdbc.core.RowMapper.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(MEMBER_ID));
	}

	@Test
	void findOpenTeamLeadConversationReturnsEmptyWhenNoRowExists() {
		when(jdbcTemplate.query(eq(AiConversationJdbcSql.SELECT_OPEN_TEAM_LEAD_CONVERSATION), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of());

		assertThat(repository.findOpenTeamLeadConversation(GROUP_ID, MEMBER_ID)).isEmpty();
	}

	@Test
	void findOpenTeamLeadConversationRejectsNullArguments() {
		assertThatThrownBy(() -> repository.findOpenTeamLeadConversation(null, MEMBER_ID))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("groupId must not be null");
		assertThatThrownBy(() -> repository.findOpenTeamLeadConversation(GROUP_ID, null))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("memberId must not be null");
	}

	@Test
	void ordinaryTeamLeadChatBuildsCurrentDbContextFromGroupWhenConversationHasNoWeek() {
		AiConversationMessageContext context = new AiConversationMessageContext(
			CONVERSATION_ID,
			GROUP_ID,
			MEMBER_ID,
			AiConversationStatus.OPEN,
			StudyGroupStatus.ACTIVE,
			GroupMemberStatus.ACTIVE
		);
		when(jdbcTemplate.query(eq(AiConversationJdbcSql.SELECT_RECENT_MESSAGES_FOR_PROMPT), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of());
		when(jdbcTemplate.query(
			argThat(sql -> sql != null && sql.contains("from study_group sg") && sql.contains("sg.topic")),
			any(org.springframework.jdbc.core.RowMapper.class),
			any(Object[].class)
		)).thenReturn(List.of(Map.of(
			"status", "AVAILABLE",
			"name", "백엔드 스터디",
			"topic", "Spring Boot",
			"detailKeywords", List.of("JPA", "백엔드"),
			"level", "INTERMEDIATE"
		)));
		when(jdbcTemplate.query(
			argThat(sql -> sql != null && sql.contains("from curriculum c") && !sql.contains("curriculum_week")),
			any(org.springframework.jdbc.core.RowMapper.class),
			any(Object[].class)
		)).thenReturn(List.of(Map.of(
			"status", "AVAILABLE",
			"title", "백엔드 커리큘럼",
			"totalWeeks", 4,
			"onboardingSummary", "Spring Boot와 JPA를 함께 학습합니다."
		)));
		when(jdbcTemplate.query(
			argThat(sql -> sql != null && sql.contains("cw.status = 'IN_PROGRESS'")),
			any(org.springframework.jdbc.core.RowMapper.class),
			any(Object[].class)
		)).thenReturn(List.of(Map.of(
			"status", "AVAILABLE",
			"id", CURRENT_WEEK_ID.toString(),
			"weekNumber", 2,
			"title", "2주차",
			"weekStatus", "IN_PROGRESS"
		)));
		when(jdbcTemplate.query(eq(AiConversationJdbcSql.SELECT_TASK_PROMPT_CONTEXT), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(Map.of(
				"title", "트랜잭션 실습",
				"completionStatus", "INCOMPLETE"
			)));
		when(jdbcTemplate.query(eq(AiConversationJdbcSql.SELECT_PROGRESS_PROMPT_CONTEXT), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(Map.of(
				"status", "AVAILABLE",
				"progressStatus", "INCOMPLETE"
			)));

		var result = repository.findPromptContext(context, 5);

		assertThat(result.conversation())
			.containsEntry("groupId", GROUP_ID.toString())
			.doesNotContainKey("weekId");
		assertThat(result.studyGroup())
			.containsEntry("status", "AVAILABLE")
			.containsEntry("topic", "Spring Boot");
		assertThat(result.curriculum())
			.containsEntry("status", "AVAILABLE")
			.containsEntry("title", "백엔드 커리큘럼");
		assertThat(result.week())
			.containsEntry("id", CURRENT_WEEK_ID.toString())
			.containsEntry("weekStatus", "IN_PROGRESS")
			.containsEntry("effectiveWeekSource", "CURRENT_WEEK");
		assertThat(result.tasks()).singleElement()
			.satisfies(task -> assertThat(task)
				.containsEntry("title", "트랜잭션 실습")
				.containsEntry("completionStatus", "INCOMPLETE"));
		assertThat(result.progress()).containsEntry("progressStatus", "INCOMPLETE");
		assertThat(AiConversationJdbcSql.SELECT_CURRENT_WEEK_PROMPT_CONTEXT)
			.contains("cw.curriculum_id = (")
			.contains("order by c.created_at desc, c.id desc");
		ArgumentCaptor<Object[]> currentWeekArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(
			argThat(sql -> sql != null && sql.contains("cw.status = 'IN_PROGRESS'")),
			any(org.springframework.jdbc.core.RowMapper.class),
			currentWeekArgs.capture()
		);
		assertThat(currentWeekArgs.getValue()).hasSize(1);
		assertThat(bytesEqual(currentWeekArgs.getValue()[0], GROUP_ID)).isTrue();
		ArgumentCaptor<Object[]> taskArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(
			eq(AiConversationJdbcSql.SELECT_TASK_PROMPT_CONTEXT),
			any(org.springframework.jdbc.core.RowMapper.class),
			taskArgs.capture()
		);
		assertThat(taskArgs.getValue()).hasSize(2);
		assertThat(bytesEqual(taskArgs.getValue()[0], MEMBER_ID)).isTrue();
		assertThat(bytesEqual(taskArgs.getValue()[1], CURRENT_WEEK_ID)).isTrue();
		ArgumentCaptor<Object[]> progressArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(
			eq(AiConversationJdbcSql.SELECT_PROGRESS_PROMPT_CONTEXT),
			any(org.springframework.jdbc.core.RowMapper.class),
			progressArgs.capture()
		);
		assertThat(progressArgs.getValue()).hasSize(2);
		assertThat(bytesEqual(progressArgs.getValue()[0], CURRENT_WEEK_ID)).isTrue();
		assertThat(bytesEqual(progressArgs.getValue()[1], MEMBER_ID)).isTrue();
	}

	@Test
	void explicitWeekPromptContextKeepsConversationWeekAndSkipsCurrentWeekFallback() {
		AiConversationMessageContext context = new AiConversationMessageContext(
			CONVERSATION_ID,
			GROUP_ID,
			MEMBER_ID,
			WEEK_ID,
			null,
			AiConversationType.TEAM_LEAD_CHAT,
			"",
			AiConversationStatus.OPEN,
			StudyGroupStatus.ACTIVE,
			GroupMemberStatus.ACTIVE
		);
		when(jdbcTemplate.query(eq(AiConversationJdbcSql.SELECT_RECENT_MESSAGES_FOR_PROMPT), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of());
		when(jdbcTemplate.query(
			argThat(sql -> sql != null && sql.contains("from study_group sg") && sql.contains("sg.topic")),
			any(org.springframework.jdbc.core.RowMapper.class),
			any(Object[].class)
		)).thenReturn(List.of(Map.of("status", "AVAILABLE", "topic", "Spring Boot")));
		when(jdbcTemplate.query(
			argThat(sql -> sql != null && sql.contains("from curriculum c") && !sql.contains("curriculum_week")),
			any(org.springframework.jdbc.core.RowMapper.class),
			any(Object[].class)
		)).thenReturn(List.of(Map.of("status", "AVAILABLE", "title", "백엔드 커리큘럼")));
		when(jdbcTemplate.query(eq(AiConversationJdbcSql.SELECT_WEEK_PROMPT_CONTEXT), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(Map.of(
				"status", "AVAILABLE",
				"id", WEEK_ID.toString(),
				"weekNumber", 1,
				"title", "1주차",
				"weekStatus", "COMPLETED"
			)));
		when(jdbcTemplate.query(eq(AiConversationJdbcSql.SELECT_TASK_PROMPT_CONTEXT), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of());
		when(jdbcTemplate.query(eq(AiConversationJdbcSql.SELECT_PROGRESS_PROMPT_CONTEXT), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(Map.of("status", "AVAILABLE", "progressStatus", "COMPLETED")));

		var result = repository.findPromptContext(context, 5);

		assertThat(result.week())
			.containsEntry("id", WEEK_ID.toString())
			.containsEntry("weekStatus", "COMPLETED")
			.containsEntry("effectiveWeekSource", "CONVERSATION_WEEK");
		verify(jdbcTemplate, never()).query(
			argThat(sql -> sql != null && sql.contains("cw.status = 'IN_PROGRESS'")),
			any(org.springframework.jdbc.core.RowMapper.class),
			any(Object[].class)
		);
		ArgumentCaptor<Object[]> weekArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(
			eq(AiConversationJdbcSql.SELECT_WEEK_PROMPT_CONTEXT),
			any(org.springframework.jdbc.core.RowMapper.class),
			weekArgs.capture()
		);
		assertThat(weekArgs.getValue()).hasSize(1);
		assertThat(bytesEqual(weekArgs.getValue()[0], WEEK_ID)).isTrue();
		ArgumentCaptor<Object[]> progressArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(
			eq(AiConversationJdbcSql.SELECT_PROGRESS_PROMPT_CONTEXT),
			any(org.springframework.jdbc.core.RowMapper.class),
			progressArgs.capture()
		);
		assertThat(progressArgs.getValue()).hasSize(2);
		assertThat(bytesEqual(progressArgs.getValue()[0], WEEK_ID)).isTrue();
		assertThat(bytesEqual(progressArgs.getValue()[1], MEMBER_ID)).isTrue();
	}

	@Test
	void explicitWeekPromptContextDoesNotLoadTasksOrProgressWhenWeekRowIsMissing() {
		AiConversationMessageContext context = new AiConversationMessageContext(
			CONVERSATION_ID,
			GROUP_ID,
			MEMBER_ID,
			WEEK_ID,
			null,
			AiConversationType.TEAM_LEAD_CHAT,
			"",
			AiConversationStatus.OPEN,
			StudyGroupStatus.ACTIVE,
			GroupMemberStatus.ACTIVE
		);
		when(jdbcTemplate.query(eq(AiConversationJdbcSql.SELECT_RECENT_MESSAGES_FOR_PROMPT), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of());
		when(jdbcTemplate.query(
			argThat(sql -> sql != null && sql.contains("from study_group sg") && sql.contains("sg.topic")),
			any(org.springframework.jdbc.core.RowMapper.class),
			any(Object[].class)
		)).thenReturn(List.of(Map.of("status", "AVAILABLE", "topic", "Spring Boot")));
		when(jdbcTemplate.query(
			argThat(sql -> sql != null && sql.contains("from curriculum c") && !sql.contains("curriculum_week")),
			any(org.springframework.jdbc.core.RowMapper.class),
			any(Object[].class)
		)).thenReturn(List.of(Map.of("status", "AVAILABLE", "title", "백엔드 커리큘럼")));
		when(jdbcTemplate.query(eq(AiConversationJdbcSql.SELECT_WEEK_PROMPT_CONTEXT), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of());

		var result = repository.findPromptContext(context, 5);

		assertThat(result.week())
			.containsEntry("status", "NOT_AVAILABLE")
			.containsEntry("effectiveWeekSource", "NOT_AVAILABLE");
		assertThat(result.tasks()).isEmpty();
		assertThat(result.progress()).containsEntry("status", "NOT_AVAILABLE");
		verify(jdbcTemplate, never()).query(
			eq(AiConversationJdbcSql.SELECT_TASK_PROMPT_CONTEXT),
			any(org.springframework.jdbc.core.RowMapper.class),
			any(Object[].class)
		);
		verify(jdbcTemplate, never()).query(
			eq(AiConversationJdbcSql.SELECT_PROGRESS_PROMPT_CONTEXT),
			any(org.springframework.jdbc.core.RowMapper.class),
			any(Object[].class)
		);
	}

	@Test
	void ordinaryTeamLeadChatMarksCurrentContextNotAvailableWhenNoActiveCurriculumWeekExists() {
		AiConversationMessageContext context = new AiConversationMessageContext(
			CONVERSATION_ID,
			GROUP_ID,
			MEMBER_ID,
			AiConversationStatus.OPEN,
			StudyGroupStatus.ACTIVE,
			GroupMemberStatus.ACTIVE
		);
		when(jdbcTemplate.query(eq(AiConversationJdbcSql.SELECT_RECENT_MESSAGES_FOR_PROMPT), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of());
		when(jdbcTemplate.query(
			argThat(sql -> sql != null && sql.contains("from study_group sg") && sql.contains("sg.topic")),
			any(org.springframework.jdbc.core.RowMapper.class),
			any(Object[].class)
		)).thenReturn(List.of(Map.of("status", "AVAILABLE", "topic", "Spring Boot")));
		when(jdbcTemplate.query(
			argThat(sql -> sql != null && sql.contains("from curriculum c") && !sql.contains("curriculum_week")),
			any(org.springframework.jdbc.core.RowMapper.class),
			any(Object[].class)
		)).thenReturn(List.of());
		when(jdbcTemplate.query(
			argThat(sql -> sql != null && sql.contains("cw.status = 'IN_PROGRESS'")),
			any(org.springframework.jdbc.core.RowMapper.class),
			any(Object[].class)
		)).thenReturn(List.of());

		var result = repository.findPromptContext(context, 5);

		assertThat(result.studyGroup()).containsEntry("status", "AVAILABLE");
		assertThat(result.curriculum()).containsEntry("status", "NOT_AVAILABLE");
		assertThat(result.week())
			.containsEntry("status", "NOT_AVAILABLE")
			.containsEntry("effectiveWeekSource", "NOT_AVAILABLE");
		assertThat(result.tasks()).isEmpty();
		assertThat(result.progress()).containsEntry("status", "NOT_AVAILABLE");
		verify(jdbcTemplate, never()).query(
			eq(AiConversationJdbcSql.SELECT_TASK_PROMPT_CONTEXT),
			any(org.springframework.jdbc.core.RowMapper.class),
			any(Object[].class)
		);
		verify(jdbcTemplate, never()).query(
			eq(AiConversationJdbcSql.SELECT_PROGRESS_PROMPT_CONTEXT),
			any(org.springframework.jdbc.core.RowMapper.class),
			any(Object[].class)
		);
	}

	@Test
	void insertConversationPersistsSessionScopeAndTimestamps() {
		AiConversation conversation = new AiConversation(
			CONVERSATION_ID,
			GROUP_ID,
			MEMBER_ID,
			WEEK_ID,
			RETROSPECTIVE_ID,
			AiConversationType.RETROSPECTIVE,
			AiConversationStatus.OPEN,
			"",
			NOW,
			null,
			NOW,
			NOW
		);
		when(jdbcTemplate.update(eq(AiConversationJdbcSql.INSERT_CONVERSATION), any(Object[].class))).thenReturn(1);

		assertThat(repository.insertConversation(conversation)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(AiConversationJdbcSql.INSERT_CONVERSATION), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(CONVERSATION_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) args.getValue()[2]).containsExactly(UuidBinary.toBytes(MEMBER_ID));
		assertThat((byte[]) args.getValue()[3]).containsExactly(UuidBinary.toBytes(WEEK_ID));
		assertThat((byte[]) args.getValue()[4]).containsExactly(UuidBinary.toBytes(RETROSPECTIVE_ID));
		assertThat(args.getValue()[5]).isEqualTo("RETROSPECTIVE");
		assertThat(args.getValue()[6]).isEqualTo("OPEN");
		assertThat(args.getValue()[7]).isEqualTo("");
		assertThat(args.getValue()[8]).isEqualTo(Timestamp.from(NOW));
		assertThat(args.getValue()[9]).isNull();
		assertThat(args.getValue()[10]).isEqualTo(Timestamp.from(NOW));
		assertThat(args.getValue()[11]).isEqualTo(Timestamp.from(NOW));
	}

	@Test
	void insertConversationReturnsFalseWhenNoRowIsInserted() {
		AiConversation conversation = teamLeadConversation(null, null);
		when(jdbcTemplate.update(eq(AiConversationJdbcSql.INSERT_CONVERSATION), any(Object[].class))).thenReturn(0);

		assertThat(repository.insertConversation(conversation)).isFalse();
	}

	@Test
	void insertConversationPropagatesDataAccessException() {
		AiConversation conversation = teamLeadConversation(null, null);
		when(jdbcTemplate.update(eq(AiConversationJdbcSql.INSERT_CONVERSATION), any(Object[].class)))
			.thenThrow(new DataIntegrityViolationException("duplicate key"));

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> repository.insertConversation(conversation))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("duplicate key");
	}

	@Test
	void insertConversationPersistsNullOptionalReferences() {
		AiConversation conversation = teamLeadConversation(null, null);
		when(jdbcTemplate.update(eq(AiConversationJdbcSql.INSERT_CONVERSATION), any(Object[].class))).thenReturn(1);

		assertThat(repository.insertConversation(conversation)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(AiConversationJdbcSql.INSERT_CONVERSATION), args.capture());
		assertThat(args.getValue()[3]).isNull();
		assertThat(args.getValue()[4]).isNull();
		assertThat(args.getValue()[9]).isNull();
	}

	@Test
	void findMessageContextQueriesConversationAndUser() {
		AiConversationMessageContext context = new AiConversationMessageContext(
			CONVERSATION_ID,
			GROUP_ID,
			MEMBER_ID,
			AiConversationStatus.OPEN,
			StudyGroupStatus.ACTIVE,
			GroupMemberStatus.ACTIVE
		);
		when(jdbcTemplate.query(eq(AiConversationJdbcSql.SELECT_MESSAGE_CONTEXT), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(context));

		Optional<AiConversationMessageContext> result = repository.findMessageContext(CONVERSATION_ID, USER_ID);

		assertThat(result).contains(context);
		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(AiConversationJdbcSql.SELECT_MESSAGE_CONTEXT), any(org.springframework.jdbc.core.RowMapper.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(CONVERSATION_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(USER_ID));
	}

	@Test
	void insertMessagePersistsSenderContentMetadataAndLlmUsage() {
		AiConversationMessage message = new AiConversationMessage(
			MESSAGE_ID,
			CONVERSATION_ID,
			LLM_USAGE_ID,
			AiConversationMessageSenderType.ASSISTANT,
			"다음 주에는 필수 과제를 하나 줄이는 방향을 추천합니다.",
			Map.of("retrievalContextVersion", "db-first-v1"),
			NOW
		);
		when(jdbcTemplate.update(eq(AiConversationJdbcSql.INSERT_MESSAGE), any(Object[].class))).thenReturn(1);

		assertThat(repository.insertMessage(message)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(AiConversationJdbcSql.INSERT_MESSAGE), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(MESSAGE_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(CONVERSATION_ID));
		assertThat((byte[]) args.getValue()[2]).containsExactly(UuidBinary.toBytes(LLM_USAGE_ID));
		assertThat(args.getValue()[3]).isEqualTo("ASSISTANT");
		assertThat(args.getValue()[4]).isEqualTo("다음 주에는 필수 과제를 하나 줄이는 방향을 추천합니다.");
		assertThat((String) args.getValue()[5]).contains("\"retrievalContextVersion\":\"db-first-v1\"");
		assertThat(args.getValue()[6]).isEqualTo(Timestamp.from(NOW));
	}

	@Test
	void findMessagesUsesCursorAndLimit() {
		AiConversationMessage message = AiConversationMessage.userMessage(
			MESSAGE_ID,
			CONVERSATION_ID,
			"사용자 메시지",
			NOW
		);
		AiConversationMessageCursor cursor = new AiConversationMessageCursor(NOW.minusSeconds(1), MESSAGE_ID);
		when(jdbcTemplate.query(eq(AiConversationJdbcSql.SELECT_MESSAGES), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(message));

		List<AiConversationMessage> result = repository.findMessages(CONVERSATION_ID, cursor, 21);

		assertThat(result).containsExactly(message);
		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(AiConversationJdbcSql.SELECT_MESSAGES), any(org.springframework.jdbc.core.RowMapper.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(CONVERSATION_ID));
		assertThat(args.getValue()[1]).isEqualTo(Timestamp.from(cursor.createdAt()));
		assertThat(args.getValue()[2]).isEqualTo(Timestamp.from(cursor.createdAt()));
		assertThat(args.getValue()[3]).isEqualTo(Timestamp.from(cursor.createdAt()));
		assertThat((byte[]) args.getValue()[4]).containsExactly(UuidBinary.toBytes(cursor.id()));
		assertThat(args.getValue()[5]).isEqualTo(21);
	}

	@Test
	void updateConversationSummaryUpdatesSummaryAndTimestamp() {
		when(jdbcTemplate.update(eq(AiConversationJdbcSql.UPDATE_CONVERSATION_SUMMARY), any(Object[].class))).thenReturn(1);

		assertThat(repository.updateConversationSummary(CONVERSATION_ID, "  새 요약  ", NOW)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(AiConversationJdbcSql.UPDATE_CONVERSATION_SUMMARY), args.capture());
		assertThat(args.getValue()[0]).isEqualTo("새 요약");
		assertThat(args.getValue()[1]).isEqualTo(Timestamp.from(NOW));
		assertThat((byte[]) args.getValue()[2]).containsExactly(UuidBinary.toBytes(CONVERSATION_ID));
	}

	private static AiConversation teamLeadConversation(UUID weekId, UUID retrospectiveId) {
		return new AiConversation(
			CONVERSATION_ID,
			GROUP_ID,
			MEMBER_ID,
			weekId,
			retrospectiveId,
			AiConversationType.TEAM_LEAD_CHAT,
			AiConversationStatus.OPEN,
			"",
			NOW,
			null,
			NOW,
			NOW
		);
	}

	private static boolean bytesEqual(Object value, UUID uuid) {
		return value instanceof byte[] bytes && java.util.Arrays.equals(bytes, UuidBinary.toBytes(uuid));
	}
}
