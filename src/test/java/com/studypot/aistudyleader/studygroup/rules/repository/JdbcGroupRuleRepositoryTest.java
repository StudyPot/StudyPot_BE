package com.studypot.aistudyleader.studygroup.rules.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.rules.domain.GroupRule;
import com.studypot.aistudyleader.studygroup.rules.domain.GroupRuleMembership;
import com.studypot.aistudyleader.studygroup.rules.domain.GroupRuleType;
import com.studypot.aistudyleader.studygroup.rules.domain.RuleViolation;
import com.studypot.aistudyleader.studygroup.rules.domain.RuleViolationStatus;
import com.studypot.aistudyleader.studygroup.rules.domain.RuleViolationType;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcGroupRuleRepositoryTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000005101");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000005102");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000005103");
	private static final UUID RULE_ID = UUID.fromString("018f0000-0000-7000-8000-000000005104");
	private static final UUID VIOLATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000005105");
	private static final UUID TASK_COMPLETION_ID = UUID.fromString("018f0000-0000-7000-8000-000000005106");
	private static final Instant NOW = Instant.parse("2026-05-12T01:00:00Z");

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final JdbcGroupRuleRepository repository = new JdbcGroupRuleRepository(jdbcTemplate, new ObjectMapper());

	@Test
	void existsStudyGroupQueriesLiveGroupByUuid() {
		when(jdbcTemplate.queryForObject(eq(GroupRuleJdbcSql.EXISTS_STUDY_GROUP), eq(Boolean.class), any(Object[].class)))
			.thenReturn(true);

		assertThat(repository.existsStudyGroup(GROUP_ID)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).queryForObject(eq(GroupRuleJdbcSql.EXISTS_STUDY_GROUP), eq(Boolean.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
	}

	@Test
	void membershipSqlFiltersLiveGroupMemberRows() {
		assertThat(GroupRuleJdbcSql.SELECT_MEMBERSHIP)
			.contains("gm.group_id = ?")
			.contains("gm.user_id = ?")
			.contains("sg.deleted_at is null")
			.contains("gm.deleted_at is null");
	}

	@Test
	void ruleUpsertSelectUsesGroupTypeAndRowLock() {
		assertThat(GroupRuleJdbcSql.SELECT_RULE_BY_GROUP_TYPE_FOR_UPDATE)
			.contains("group_id = ?")
			.contains("rule_type = ?")
			.contains("deleted_at is null")
			.contains("for update");
	}

	@Test
	void insertRuleSerializesUuidAndConfigJson() {
		GroupRule rule = rule(GroupRuleType.TASK_DEADLINE, true);

		when(jdbcTemplate.update(eq(GroupRuleJdbcSql.INSERT_RULE), any(Object[].class))).thenReturn(1);

		assertThat(repository.insertRule(rule)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(GroupRuleJdbcSql.INSERT_RULE), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(RULE_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) args.getValue()[2]).containsExactly(UuidBinary.toBytes(USER_ID));
		assertThat(args.getValue()[3]).isEqualTo("TASK_DEADLINE");
		assertThat(args.getValue()[4].toString()).contains("\"dueTime\":\"23:59\"");
		assertThat(args.getValue()[5]).isEqualTo("금요일 자정 전까지 제출");
		assertThat(args.getValue()[6]).isEqualTo(true);
		assertThat(args.getValue()[7]).isEqualTo(Timestamp.from(NOW));
	}

	@Test
	void deactivateRuleUpdatesOnlyLiveRuleInGroup() {
		when(jdbcTemplate.update(eq(GroupRuleJdbcSql.DEACTIVATE_RULE), any(Object[].class))).thenReturn(1);

		assertThat(repository.deactivateRule(GROUP_ID, RULE_ID, NOW)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(GroupRuleJdbcSql.DEACTIVATE_RULE), args.capture());
		assertThat(args.getValue()[0]).isEqualTo(Timestamp.from(NOW));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) args.getValue()[2]).containsExactly(UuidBinary.toBytes(RULE_ID));
	}

	@Test
	void taskCompletionBelongsToMemberUsesTaskAndMemberIds() {
		when(jdbcTemplate.queryForObject(eq(GroupRuleJdbcSql.EXISTS_TASK_COMPLETION_FOR_MEMBER), eq(Boolean.class), any(Object[].class)))
			.thenReturn(true);

		assertThat(repository.taskCompletionBelongsToMember(TASK_COMPLETION_ID, MEMBER_ID)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).queryForObject(eq(GroupRuleJdbcSql.EXISTS_TASK_COMPLETION_FOR_MEMBER), eq(Boolean.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(TASK_COMPLETION_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(MEMBER_ID));
	}

	@Test
	void existsTaskCompletionQueriesByTaskCompletionId() {
		when(jdbcTemplate.queryForObject(eq(GroupRuleJdbcSql.EXISTS_TASK_COMPLETION), eq(Boolean.class), any(Object[].class)))
			.thenReturn(true);

		assertThat(repository.existsTaskCompletion(TASK_COMPLETION_ID)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).queryForObject(eq(GroupRuleJdbcSql.EXISTS_TASK_COMPLETION), eq(Boolean.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(TASK_COMPLETION_ID));
	}

	@Test
	void insertViolationStoresViolationTypeInsideDetailsJson() {
		RuleViolation violation = violation(RuleViolationStatus.OPEN);

		repository.insertViolation(violation);

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(GroupRuleJdbcSql.INSERT_VIOLATION), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(VIOLATION_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(RULE_ID));
		assertThat((byte[]) args.getValue()[2]).containsExactly(UuidBinary.toBytes(MEMBER_ID));
		assertThat((byte[]) args.getValue()[3]).containsExactly(UuidBinary.toBytes(TASK_COMPLETION_ID));
		assertThat(args.getValue()[4].toString())
			.contains("\"violationType\":\"INCOMPLETE_REASON_MISSING\"")
			.contains("\"reason\":\"missing\"");
		assertThat(args.getValue()[5]).isEqualTo("OPEN");
		assertThat(args.getValue()[8]).isEqualTo(Timestamp.from(NOW.minusSeconds(60)));
	}

	@Test
	void insertViolationRejectsDetailsWithReservedViolationTypeKey() {
		RuleViolation violation = new RuleViolation(
			VIOLATION_ID,
			RULE_ID,
			MEMBER_ID,
			Optional.empty(),
			RuleViolationType.CUSTOM,
			Map.of("violationType", "RETROSPECTIVE_MISSING"),
			RuleViolationStatus.OPEN,
			Optional.empty(),
			Optional.empty(),
			NOW.minusSeconds(60),
			NOW
		);

		assertThatThrownBy(() -> repository.insertViolation(violation))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("details must not contain reserved key: violationType");
	}

	@Test
	void updateViolationStatusPersistsResolvedStateAndNote() {
		RuleViolation resolved = violation(RuleViolationStatus.OPEN).resolve("처리 완료", NOW);
		when(jdbcTemplate.update(eq(GroupRuleJdbcSql.UPDATE_VIOLATION_STATUS), any(Object[].class))).thenReturn(1);

		assertThat(repository.updateViolationStatus(resolved)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(GroupRuleJdbcSql.UPDATE_VIOLATION_STATUS), args.capture());
		assertThat(args.getValue()[0]).isEqualTo("RESOLVED");
		assertThat(args.getValue()[1]).isEqualTo(Timestamp.from(NOW));
		assertThat(args.getValue()[2]).isEqualTo("처리 완료");
		assertThat((byte[]) args.getValue()[3]).containsExactly(UuidBinary.toBytes(VIOLATION_ID));
	}

	@Test
	void mapMembershipReadsPermissionAndStatus() throws Exception {
		ResultSet resultSet = mock(ResultSet.class);
		when(resultSet.getBytes("group_id")).thenReturn(UuidBinary.toBytes(GROUP_ID));
		when(resultSet.getBytes("member_id")).thenReturn(UuidBinary.toBytes(MEMBER_ID));
		when(resultSet.getString("permission")).thenReturn("MEMBER");
		when(resultSet.getString("member_status")).thenReturn("ACTIVE");

		GroupRuleMembership result = repository.mapMembership(resultSet, 0);

		assertThat(result.groupId()).isEqualTo(GROUP_ID);
		assertThat(result.memberId()).isEqualTo(MEMBER_ID);
		assertThat(result.permission()).isEqualTo(GroupMemberPermission.MEMBER);
		assertThat(result.status()).isEqualTo(GroupMemberStatus.ACTIVE);
	}

	@Test
	void mapRuleReadsConfigAndDeletedAt() throws Exception {
		ResultSet resultSet = mock(ResultSet.class);
		when(resultSet.getBytes("id")).thenReturn(UuidBinary.toBytes(RULE_ID));
		when(resultSet.getBytes("group_id")).thenReturn(UuidBinary.toBytes(GROUP_ID));
		when(resultSet.getBytes("created_by")).thenReturn(UuidBinary.toBytes(USER_ID));
		when(resultSet.getString("rule_type")).thenReturn("CUSTOM_NOTE");
		when(resultSet.getString("config")).thenReturn("{\"memo\":\"지각 시 미리 공유\"}");
		when(resultSet.getString("description")).thenReturn("운영 메모");
		when(resultSet.getBoolean("is_active")).thenReturn(true);
		when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(NOW.minusSeconds(120)));
		when(resultSet.getTimestamp("updated_at")).thenReturn(Timestamp.from(NOW.minusSeconds(60)));
		when(resultSet.getTimestamp("deleted_at")).thenReturn(Timestamp.from(NOW));

		GroupRule result = repository.mapRule(resultSet, 0);

		assertThat(result.ruleType()).isEqualTo(GroupRuleType.CUSTOM_NOTE);
		assertThat(result.config()).containsEntry("memo", "지각 시 미리 공유");
		assertThat(result.deletedAt()).contains(NOW);
	}

	@Test
	void mapViolationReadsViolationTypeFromDetailsJson() throws Exception {
		ResultSet resultSet = mock(ResultSet.class);
		when(resultSet.getBytes("id")).thenReturn(UuidBinary.toBytes(VIOLATION_ID));
		when(resultSet.getBytes("rule_id")).thenReturn(UuidBinary.toBytes(RULE_ID));
		when(resultSet.getBytes("member_id")).thenReturn(UuidBinary.toBytes(MEMBER_ID));
		when(resultSet.getBytes("task_completion_id")).thenReturn(null);
		when(resultSet.getString("details")).thenReturn("{\"violationType\":\"RETROSPECTIVE_MISSING\",\"reason\":\"not submitted\"}");
		when(resultSet.getString("status")).thenReturn("WAIVED");
		when(resultSet.getTimestamp("resolved_at")).thenReturn(Timestamp.from(NOW));
		when(resultSet.getString("resolved_note")).thenReturn("면제");
		when(resultSet.getTimestamp("occurred_at")).thenReturn(Timestamp.from(NOW.minusSeconds(60)));
		when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(NOW.minusSeconds(30)));

		RuleViolation result = repository.mapViolation(resultSet, 0);

		assertThat(result.violationType()).isEqualTo(RuleViolationType.RETROSPECTIVE_MISSING);
		assertThat(result.details()).containsEntry("reason", "not submitted");
		assertThat(result.details()).doesNotContainKey("violationType");
		assertThat(result.status()).isEqualTo(RuleViolationStatus.WAIVED);
		assertThat(result.resolvedNote()).contains("면제");
	}

	@Test
	void mapViolationRejectsInvalidViolationTypeInDetailsJson() throws Exception {
		ResultSet resultSet = mock(ResultSet.class);
		when(resultSet.getBytes("id")).thenReturn(UuidBinary.toBytes(VIOLATION_ID));
		when(resultSet.getBytes("rule_id")).thenReturn(UuidBinary.toBytes(RULE_ID));
		when(resultSet.getBytes("member_id")).thenReturn(UuidBinary.toBytes(MEMBER_ID));
		when(resultSet.getBytes("task_completion_id")).thenReturn(null);
		when(resultSet.getString("details")).thenReturn("{\"violationType\":\"UNKNOWN\"}");
		when(resultSet.getString("status")).thenReturn("OPEN");
		when(resultSet.getTimestamp("resolved_at")).thenReturn(null);
		when(resultSet.getString("resolved_note")).thenReturn(null);
		when(resultSet.getTimestamp("occurred_at")).thenReturn(Timestamp.from(NOW.minusSeconds(60)));
		when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(NOW.minusSeconds(30)));

		assertThatThrownBy(() -> repository.mapViolation(resultSet, 0))
			.isInstanceOf(GroupRulePersistenceException.class)
			.hasMessageContaining("invalid violationType: UNKNOWN");
	}

	private static GroupRule rule(GroupRuleType type, boolean active) {
		return new GroupRule(
			RULE_ID,
			GROUP_ID,
			USER_ID,
			type,
			Map.of("dueTime", "23:59"),
			"금요일 자정 전까지 제출",
			active,
			NOW,
			NOW,
			Optional.empty()
		);
	}

	private static RuleViolation violation(RuleViolationStatus status) {
		return new RuleViolation(
			VIOLATION_ID,
			RULE_ID,
			MEMBER_ID,
			Optional.of(TASK_COMPLETION_ID),
			RuleViolationType.INCOMPLETE_REASON_MISSING,
			Map.of("reason", "missing"),
			status,
			Optional.empty(),
			Optional.empty(),
			NOW.minusSeconds(60),
			NOW
		);
	}
}
