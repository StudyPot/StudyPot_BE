package com.studypot.aistudyleader.curriculum.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.curriculum.domain.Curriculum;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGeneration;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStartContext;
import com.studypot.aistudyleader.curriculum.domain.CurriculumTaskPlan;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeekPlan;
import com.studypot.aistudyleader.curriculum.domain.LlmProvider;
import com.studypot.aistudyleader.curriculum.domain.LlmUsage;
import com.studypot.aistudyleader.curriculum.domain.LlmUsageStatus;
import com.studypot.aistudyleader.curriculum.domain.SubmittedOnboardingResponse;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcCurriculumRepositoryTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000004221");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000004222");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000004223");
	private static final UUID RESPONSE_ID = UUID.fromString("018f0000-0000-7000-8000-000000004224");
	private static final UUID LLM_USAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000004225");
	private static final UUID CURRICULUM_ID = UUID.fromString("018f0000-0000-7000-8000-000000004226");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000004227");
	private static final UUID TASK_ID = UUID.fromString("018f0000-0000-7000-8000-000000004228");
	private static final Instant NOW = Instant.parse("2026-05-11T03:00:00Z");

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final JdbcCurriculumRepository repository = new JdbcCurriculumRepository(jdbcTemplate, new ObjectMapper());

	@Test
	void existsStudyGroupReturnsTrueWhenGroupExists() {
		when(jdbcTemplate.queryForObject(eq(CurriculumJdbcSql.EXISTS_STUDY_GROUP), eq(Boolean.class), any(Object[].class)))
			.thenReturn(true);

		assertThat(repository.existsStudyGroup(GROUP_ID)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).queryForObject(eq(CurriculumJdbcSql.EXISTS_STUDY_GROUP), eq(Boolean.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
	}

	@Test
	void findStartContextQueriesCurrentMemberForGroup() {
		CurriculumStartContext context = context(StudyGroupStatus.ONBOARDING, GroupMemberPermission.OWNER, GroupMemberStatus.PENDING_ONBOARDING);
		when(jdbcTemplate.query(eq(CurriculumJdbcSql.SELECT_START_CONTEXT), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(context));

		Optional<CurriculumStartContext> result = repository.findStartContext(GROUP_ID, USER_ID);

		assertThat(result).contains(context);
		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(CurriculumJdbcSql.SELECT_START_CONTEXT), any(org.springframework.jdbc.core.RowMapper.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(USER_ID));
	}

	@Test
	void submittedOnboardingSqlFiltersSubmittedRowsOnly() {
		assertThat(CurriculumJdbcSql.SELECT_SUBMITTED_ONBOARDING_RESPONSES)
			.contains("gor.status = 'SUBMITTED'")
			.doesNotContain("gor.status in");
	}

	@Test
	void findSubmittedOnboardingResponsesReturnsRowsWithAvailabilitySlots() {
		SubmittedOnboardingResponse response = submittedResponse(List.of());
		when(jdbcTemplate.query(eq(CurriculumJdbcSql.SELECT_SUBMITTED_ONBOARDING_RESPONSES), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(response));
		when(jdbcTemplate.query(eq(CurriculumJdbcSql.SELECT_SUBMITTED_AVAILABILITY_SLOTS), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of());

		List<SubmittedOnboardingResponse> result = repository.findSubmittedOnboardingResponses(GROUP_ID);

		assertThat(result).containsExactly(response);
		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(CurriculumJdbcSql.SELECT_SUBMITTED_ONBOARDING_RESPONSES), any(org.springframework.jdbc.core.RowMapper.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
	}

	@Test
	void saveStartedCurriculumPersistsGroupTransitionAndGeneratedRows() {
		when(jdbcTemplate.update(eq(CurriculumJdbcSql.UPDATE_STUDY_GROUP_STARTED), any(Object[].class))).thenReturn(1);
		CurriculumGeneration generation = generation();
		LlmUsage usage = generation.toLlmUsage(LLM_USAGE_ID, USER_ID, GROUP_ID, NOW);
		Curriculum curriculum = generation.toCurriculum(
			CURRICULUM_ID,
			GROUP_ID,
			LLM_USAGE_ID,
			Map.of("submittedResponseCount", 1),
			NOW,
			List.of(WEEK_ID),
			List.of(TASK_ID)
		);

		repository.saveStartedCurriculum(GROUP_ID, NOW, usage, curriculum);

		ArgumentCaptor<Object[]> groupArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(CurriculumJdbcSql.UPDATE_STUDY_GROUP_STARTED), groupArgs.capture());
		assertThat(groupArgs.getValue()[0]).isEqualTo(Timestamp.from(NOW));
		assertThat(groupArgs.getValue()[2]).isEqualTo(UuidBinary.toBytes(GROUP_ID));

		ArgumentCaptor<Object[]> usageArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(CurriculumJdbcSql.INSERT_LLM_USAGE), usageArgs.capture());
		assertThat((byte[]) usageArgs.getValue()[0]).containsExactly(UuidBinary.toBytes(LLM_USAGE_ID));
		assertThat(usageArgs.getValue()[3]).isEqualTo("CURRICULUM_GENERATE");
		assertThat(usageArgs.getValue()[4]).isEqualTo("OPENAI");

		ArgumentCaptor<Object[]> curriculumArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(CurriculumJdbcSql.INSERT_CURRICULUM), curriculumArgs.capture());
		assertThat((byte[]) curriculumArgs.getValue()[0]).containsExactly(UuidBinary.toBytes(CURRICULUM_ID));
		assertThat((byte[]) curriculumArgs.getValue()[1]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) curriculumArgs.getValue()[2]).containsExactly(UuidBinary.toBytes(LLM_USAGE_ID));
		assertThat(curriculumArgs.getValue()[3]).isEqualTo("Spring Boot 6주 완성");
		assertThat(curriculumArgs.getValue()[5].toString()).contains("submittedResponseCount");

		ArgumentCaptor<Object[]> weekArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(CurriculumJdbcSql.INSERT_CURRICULUM_WEEK), weekArgs.capture());
		assertThat((byte[]) weekArgs.getValue()[0]).containsExactly(UuidBinary.toBytes(WEEK_ID));
		assertThat((byte[]) weekArgs.getValue()[1]).containsExactly(UuidBinary.toBytes(CURRICULUM_ID));
		assertThat(weekArgs.getValue()[2]).isEqualTo(1);

		ArgumentCaptor<Object[]> taskArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(CurriculumJdbcSql.INSERT_WEEKLY_TASK), taskArgs.capture());
		assertThat((byte[]) taskArgs.getValue()[0]).containsExactly(UuidBinary.toBytes(TASK_ID));
		assertThat((byte[]) taskArgs.getValue()[1]).containsExactly(UuidBinary.toBytes(WEEK_ID));
		assertThat(taskArgs.getValue()[3]).isEqualTo("READING");
	}

	@Test
	void saveStartedCurriculumRejectsWhenGroupWasAlreadyStartedByRace() {
		when(jdbcTemplate.update(eq(CurriculumJdbcSql.UPDATE_STUDY_GROUP_STARTED), any(Object[].class))).thenReturn(0);
		CurriculumGeneration generation = generation();

		assertThatThrownBy(() -> repository.saveStartedCurriculum(
				GROUP_ID,
				NOW,
				generation.toLlmUsage(LLM_USAGE_ID, USER_ID, GROUP_ID, NOW),
				generation.toCurriculum(CURRICULUM_ID, GROUP_ID, LLM_USAGE_ID, Map.of(), NOW, List.of(WEEK_ID), List.of(TASK_ID))
			))
			.isInstanceOf(CurriculumPersistenceException.class)
			.hasMessage("study group could not be transitioned to ACTIVE.");
	}

	private static CurriculumStartContext context(
		StudyGroupStatus groupStatus,
		GroupMemberPermission permission,
		GroupMemberStatus memberStatus
	) {
		return new CurriculumStartContext(
			GROUP_ID,
			"Backend Interview Study",
			"Spring Boot",
			List.of("JPA", "Security"),
			groupStatus,
			LocalDate.parse("2026-05-11"),
			LocalDate.parse("2026-06-21"),
			MEMBER_ID,
			permission,
			memberStatus
		);
	}

	private static SubmittedOnboardingResponse submittedResponse(List<com.studypot.aistudyleader.curriculum.domain.SubmittedAvailabilitySlot> slots) {
		return new SubmittedOnboardingResponse(
			RESPONSE_ID,
			MEMBER_ID,
			Map.of("JPA", 2),
			Map.of("READING", 4),
			null,
			slots,
			Instant.parse("2026-05-10T08:00:00Z")
		);
	}

	private static CurriculumGeneration generation() {
		return new CurriculumGeneration(
			"Spring Boot 6주 완성",
			List.of(new CurriculumWeekPlan(
				1,
				"JPA 기초",
				"핵심 개념을 맞춥니다.",
				List.of("Entity 매핑 이해"),
				List.of(),
				List.of(new CurriculumTaskPlan(WeeklyTaskType.READING, "JPA 읽기", null, true))
			)),
			"Generate a curriculum as JSON.",
			LlmProvider.OPENAI,
			"gpt-4o-mini",
			10,
			20,
			BigDecimal.ZERO,
			100,
			LlmUsageStatus.SUCCESS,
			null,
			Map.of("purpose", "CURRICULUM_GENERATE"),
			"Generated."
		);
	}
}
