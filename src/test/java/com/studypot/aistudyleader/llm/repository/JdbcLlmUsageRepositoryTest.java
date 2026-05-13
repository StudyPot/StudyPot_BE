package com.studypot.aistudyleader.llm.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsage;
import com.studypot.aistudyleader.llm.domain.LlmUsageAccessContext;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcLlmUsageRepositoryTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000007201");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000007202");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000007203");
	private static final UUID USAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000007204");
	private static final Instant NOW = Instant.parse("2026-05-13T02:30:00Z");

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final JdbcLlmUsageRepository repository = new JdbcLlmUsageRepository(jdbcTemplate, new ObjectMapper());

	@Test
	void insertLlmUsagePersistsAuditColumnsWithRedactedPayload() {
		when(jdbcTemplate.update(eq(LlmUsageJdbcSql.INSERT_LLM_USAGE), any(Object[].class))).thenReturn(1);
		LlmUsage usage = usage(Map.of("apiKey", "sk-raw", "source", "chat"));

		assertThat(repository.insertLlmUsage(usage)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(LlmUsageJdbcSql.INSERT_LLM_USAGE), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(USAGE_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(USER_ID));
		assertThat((byte[]) args.getValue()[2]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat(args.getValue()[3]).isEqualTo("TEAM_LEAD_CHAT");
		assertThat(args.getValue()[4]).isEqualTo("OPENAI");
		assertThat(args.getValue()[5]).isEqualTo("gpt-4.1-mini");
		assertThat(args.getValue()[6]).isEqualTo(120);
		assertThat(args.getValue()[7]).isEqualTo(45);
		assertThat(args.getValue()[8]).isEqualTo(new BigDecimal("0.000321"));
		assertThat(args.getValue()[9]).isEqualTo(230);
		assertThat(args.getValue()[10]).isEqualTo("SUCCESS");
		assertThat(args.getValue()[11]).isNull();
		assertThat(args.getValue()[12].toString())
			.contains("\"apiKey\":\"[REDACTED]\"")
			.doesNotContain("sk-raw");
		assertThat(args.getValue()[13]).isEqualTo("assistant response summary");
		assertThat(args.getValue()[14]).isEqualTo(Date.valueOf("2026-05-13"));
		assertThat(args.getValue()[15]).isEqualTo(Timestamp.from(NOW));
	}

	@Test
	void accessContextSqlFiltersGroupAndUserThroughNonDeletedMembership() {
		assertThat(LlmUsageJdbcSql.SELECT_ACCESS_CONTEXT)
			.contains("from study_group sg")
			.contains("join group_member gm on gm.group_id = sg.id")
			.contains("where sg.id = ?")
			.contains("and gm.user_id = ?")
			.contains("sg.deleted_at is null")
			.contains("gm.deleted_at is null");
	}

	@Test
	void findAccessContextQueriesGroupAndUser() {
		LlmUsageAccessContext context = new LlmUsageAccessContext(
			GROUP_ID,
			MEMBER_ID,
			StudyGroupStatus.ACTIVE,
			GroupMemberPermission.OWNER,
			GroupMemberStatus.ACTIVE
		);
		when(jdbcTemplate.query(eq(LlmUsageJdbcSql.SELECT_ACCESS_CONTEXT), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(context));

		Optional<LlmUsageAccessContext> result = repository.findAccessContext(GROUP_ID, USER_ID);

		assertThat(result).contains(context);
		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(LlmUsageJdbcSql.SELECT_ACCESS_CONTEXT), any(org.springframework.jdbc.core.RowMapper.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(USER_ID));
	}

	@Test
	void findGroupAndUserUsageUseDateIndexesAndStableOrdering() {
		assertThat(LlmUsageJdbcSql.SELECT_GROUP_USAGE)
			.contains("where group_id = ?")
			.contains("order by created_at desc, id desc")
			.contains("limit ?");
		assertThat(LlmUsageJdbcSql.SELECT_USER_USAGE)
			.contains("where user_id = ?")
			.contains("order by created_at desc, id desc")
			.contains("limit ?");

		LlmUsage usage = usage(Map.of("source", "chat"));
		when(jdbcTemplate.query(eq(LlmUsageJdbcSql.SELECT_GROUP_USAGE), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(usage));
		when(jdbcTemplate.query(eq(LlmUsageJdbcSql.SELECT_USER_USAGE), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(usage));

		assertThat(repository.findGroupUsage(GROUP_ID, 50)).containsExactly(usage);
		assertThat(repository.findUserUsage(USER_ID, 50)).containsExactly(usage);
	}

	private static LlmUsage usage(Map<String, Object> requestPayload) {
		return LlmUsage.record(
			USAGE_ID,
			USER_ID,
			GROUP_ID,
			LlmUsagePurpose.TEAM_LEAD_CHAT,
			LlmProvider.OPENAI,
			"gpt-4.1-mini",
			120,
			45,
			new BigDecimal("0.000321"),
			230,
			LlmUsageStatus.SUCCESS,
			null,
			requestPayload,
			"assistant response summary",
			NOW
		);
	}
}
