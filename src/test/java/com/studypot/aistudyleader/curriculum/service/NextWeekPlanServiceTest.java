package com.studypot.aistudyleader.curriculum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studypot.aistudyleader.curriculum.domain.CurriculumStartContext;
import com.studypot.aistudyleader.curriculum.domain.CurriculumTaskPlan;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeek;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeekStatus;
import com.studypot.aistudyleader.curriculum.domain.NextWeekTarget;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTask;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import com.studypot.aistudyleader.curriculum.repository.CurriculumRepository;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NextWeekPlanServiceTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000009101");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000009102");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000009103");
	private static final UUID NEXT_WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000009104");
	private static final UUID CURRICULUM_ID = UUID.fromString("018f0000-0000-7000-8000-000000009105");
	private static final Instant NOW = Instant.parse("2026-06-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	private final CurriculumRepository repository = mock(CurriculumRepository.class);

	@Test
	void regenerateNextWeekReplacesTasksAndPromptForOwner() {
		when(repository.findReadContextByWeekId(WEEK_ID, USER_ID)).thenReturn(Optional.of(ownerContext()));
		when(repository.findNextPendingWeek(WEEK_ID)).thenReturn(Optional.of(new NextWeekTarget(NEXT_WEEK_ID, 2, "JPA 심화", "목표")));
		when(repository.findLatestWeeklyReportBody(GROUP_ID)).thenReturn(Optional.of("지난 주 리포트"));
		CurriculumWeek expected = week();
		when(repository.replaceNextWeekTasks(eq(NEXT_WEEK_ID), any(), any(), eq(NOW))).thenReturn(expected);

		NextWeekPlanGenerator generator = input -> new NextWeekPlanGeneration(
			new NextWeekPlan(List.of(new CurriculumTaskPlan(WeeklyTaskType.PRACTICE, "실습", "설명", true)), "회고 질문"),
			response()
		);
		NextWeekPlanService service = new NextWeekPlanService(repository, () -> generator, () -> mock(LlmUsageRecorder.class), CLOCK, fixedIds());

		CurriculumWeek result = service.regenerateNextWeek(new RegenerateNextWeekCommand(USER_ID, GROUP_ID, WEEK_ID));

		assertThat(result).isSameAs(expected);
		ArgumentCaptor<List<WeeklyTask>> tasks = ArgumentCaptor.forClass(List.class);
		verify(repository).replaceNextWeekTasks(eq(NEXT_WEEK_ID), tasks.capture(), eq("회고 질문"), eq(NOW));
		assertThat(tasks.getValue()).hasSize(1);
		assertThat(tasks.getValue().getFirst().curriculumWeekId()).isEqualTo(NEXT_WEEK_ID);
	}

	@Test
	void regenerateNextWeekRejectsNonOwner() {
		CurriculumStartContext member = new CurriculumStartContext(
			GROUP_ID, "g", "topic", List.of("k"), StudyGroupStatus.ACTIVE,
			LocalDate.parse("2026-05-11"), LocalDate.parse("2026-06-21"),
			UUID.randomUUID(), GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE
		);
		when(repository.findReadContextByWeekId(WEEK_ID, USER_ID)).thenReturn(Optional.of(member));
		NextWeekPlanService service = new NextWeekPlanService(
			repository, () -> mock(NextWeekPlanGenerator.class), () -> mock(LlmUsageRecorder.class), CLOCK, fixedIds()
		);

		assertThatThrownBy(() -> service.regenerateNextWeek(new RegenerateNextWeekCommand(USER_ID, GROUP_ID, WEEK_ID)))
			.isInstanceOf(CurriculumAccessDeniedException.class);
	}

	private static CurriculumStartContext ownerContext() {
		return new CurriculumStartContext(
			GROUP_ID, "g", "topic", List.of("k"), StudyGroupStatus.ACTIVE,
			LocalDate.parse("2026-05-11"), LocalDate.parse("2026-06-21"),
			UUID.randomUUID(), GroupMemberPermission.OWNER, GroupMemberStatus.ACTIVE
		);
	}

	private static CurriculumWeek week() {
		return new CurriculumWeek(
			NEXT_WEEK_ID, CURRICULUM_ID, 2, "JPA 심화", null, "목표", "회고 질문",
			List.of(), List.of(), CurriculumWeekStatus.PENDING,
			NOW, NOW.plusSeconds(604800), List.of(), NOW, NOW
		);
	}

	private static LlmStructuredResponse response() {
		return new LlmStructuredResponse(
			LlmProvider.OPENAI, "gpt-4o-mini", "{}", 10, 10, BigDecimal.ZERO, 50,
			LlmUsageStatus.SUCCESS, null, Map.of("purpose", "NEXT_WEEK_ADJUST"), "raw"
		);
	}

	private static java.util.function.Supplier<UUID> fixedIds() {
		return () -> UUID.fromString("018f0000-0000-7000-8000-0000000091aa");
	}
}
