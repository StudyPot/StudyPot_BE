package com.studypot.aistudyleader.curriculum.service;

import com.studypot.aistudyleader.curriculum.domain.CurriculumStartContext;
import com.studypot.aistudyleader.curriculum.domain.CurriculumTaskPlan;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeek;
import com.studypot.aistudyleader.curriculum.domain.NextWeekTarget;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTask;
import com.studypot.aistudyleader.curriculum.repository.CurriculumRepository;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.annotation.Transactional;

/**
 * 직전 주차 리포트를 기반으로 다음 주차 TODO + 회고 프롬프트를 AI로 재생성한다. (그룹장 전용)
 */
public class NextWeekPlanService {

	private final CurriculumRepository repository;
	private final Supplier<NextWeekPlanGenerator> generatorSupplier;
	private final Supplier<LlmUsageRecorder> usageRecorderSupplier;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;

	public NextWeekPlanService(
		CurriculumRepository repository,
		Supplier<NextWeekPlanGenerator> generatorSupplier,
		Supplier<LlmUsageRecorder> usageRecorderSupplier,
		Clock clock,
		Supplier<UUID> idGenerator
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.generatorSupplier = Objects.requireNonNull(generatorSupplier, "generatorSupplier must not be null");
		this.usageRecorderSupplier = Objects.requireNonNull(usageRecorderSupplier, "usageRecorderSupplier must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
	}

	@Transactional
	public CurriculumWeek regenerateNextWeek(RegenerateNextWeekCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		CurriculumStartContext context = repository.findReadContextByWeekId(command.weekId(), command.authenticatedUserId())
			.orElseThrow(() -> new CurriculumNotFoundException("curriculum week was not found."));
		if (!context.isOwner()) {
			throw new CurriculumAccessDeniedException("only the study group owner can regenerate the next week.");
		}
		NextWeekPlanGenerator generator = generatorSupplier.get();
		LlmUsageRecorder recorder = usageRecorderSupplier.get();
		if (generator == null || recorder == null) {
			throw new CurriculumGenerationException("next week plan generator is not configured.");
		}
		NextWeekTarget nextWeek = repository.findNextPendingWeek(command.weekId())
			.orElseThrow(() -> new CurriculumNotFoundException("no pending next week to regenerate."));
		String reportBody = repository.findLatestWeeklyReportBody(command.groupId())
			.orElseThrow(() -> new CurriculumNotFoundException("no weekly report is available to base the next week on."));

		NextWeekPlanGeneration generation = generator.generate(new NextWeekPlanInput(
			nextWeek.weekNumber(), nextWeek.title(), nextWeek.sprintGoal(), reportBody
		));
		Instant now = clock.instant();
		recorder.record(generation.response().toUsage(
			idGenerator.get(),
			command.authenticatedUserId(),
			command.groupId(),
			LlmUsagePurpose.NEXT_WEEK_ADJUST,
			now
		));

		List<WeeklyTask> tasks = toWeeklyTasks(nextWeek.weekId(), generation.plan().tasks(), now);
		return repository.replaceNextWeekTasks(nextWeek.weekId(), tasks, generation.plan().retrospectivePrompt(), now);
	}

	private List<WeeklyTask> toWeeklyTasks(UUID weekId, List<CurriculumTaskPlan> plans, Instant now) {
		List<WeeklyTask> tasks = new ArrayList<>();
		int order = 1;
		for (CurriculumTaskPlan plan : plans) {
			tasks.add(new WeeklyTask(
				idGenerator.get(),
				weekId,
				order++,
				plan.taskType(),
				plan.title(),
				plan.description(),
				plan.required(),
				null,
				true,
				Map.of("source", "NEXT_WEEK_ADJUST"),
				now,
				now
			));
		}
		return tasks;
	}
}
