package com.studypot.aistudyleader.ai.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studypot.aistudyleader.ai.domain.AiConversation;
import com.studypot.aistudyleader.ai.domain.AiConversationStatus;
import com.studypot.aistudyleader.ai.domain.AiConversationType;
import com.studypot.aistudyleader.ai.domain.AiConversationMembershipContext;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
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
	private static final UUID RETROSPECTIVE_ID = UUID.fromString("018f0000-0000-7000-8000-000000009205");
	private static final UUID CONVERSATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000009206");
	private static final Instant NOW = Instant.parse("2026-05-13T01:00:00Z");

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final JdbcAiConversationRepository repository = new JdbcAiConversationRepository(jdbcTemplate);

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
			.contains("r.id = ?");
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
}
