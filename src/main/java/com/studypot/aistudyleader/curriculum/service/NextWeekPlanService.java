package com.studypot.aistudyleader.curriculum.service;

import com.studypot.aistudyleader.curriculum.domain.Curriculum;
import com.studypot.aistudyleader.curriculum.domain.CurriculumSprintPlanner;
import com.studypot.aistudyleader.curriculum.domain.CurriculumSprintWindow;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStartContext;
import com.studypot.aistudyleader.curriculum.domain.CurriculumTaskPlan;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeek;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeekStatus;
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
import java.util.Optional;
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
		return repository.replaceNextWeekTasks(nextWeek.weekId(), tasks, generation.plan().retrospectiveQuestions(), now);
	}

	/**
	 * 주차 리포트가 게시된 직후 스케줄러가 호출하는 '다음 주차 점진 생성' 경로.
	 * 직전 주차의 TODO + 리포트 + 멤버 회고를 입력으로 AI가 다음 주차를 만든다(회고가 없으면 직전 주차 TODO만 참고).
	 * 다음 주차가 마지막 주차를 넘으면(스터디 종료) 또는 이미 존재하면(멱등) 아무것도 하지 않는다.
	 * 그룹장 인증 검사 없이 시스템이 수행하며, 사용량은 actorUserId(그룹장)에게 귀속한다.
	 */
	@Transactional
	public void createNextWeekAutomatically(UUID groupId, UUID currentWeekId, UUID actorUserId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(currentWeekId, "currentWeekId must not be null");
		Objects.requireNonNull(actorUserId, "actorUserId must not be null");
		NextWeekPlanGenerator generator = generatorSupplier.get();
		LlmUsageRecorder recorder = usageRecorderSupplier.get();
		if (generator == null || recorder == null) {
			return;
		}
		Instant now = clock.instant();

		// 그룹 기간(전체 주차 계획) + 현재 커리큘럼
		Optional<CurriculumStartContext> contextOpt = repository.findReadContextByWeekId(currentWeekId, actorUserId);
		Optional<Curriculum> curriculumOpt = repository.findActiveCurriculumByGroupId(groupId);
		if (contextOpt.isEmpty() || curriculumOpt.isEmpty()) {
			return;
		}
		CurriculumStartContext context = contextOpt.get();
		Curriculum curriculum = curriculumOpt.get();
		List<CurriculumSprintWindow> windows = CurriculumSprintPlanner.fixedWeeklyWindows(context.startsAt(), context.endsAt());
		int maxWeekNumber = curriculum.weeks().stream().mapToInt(CurriculumWeek::weekNumber).max().orElse(0);
		int nextWeekNumber = maxWeekNumber + 1;
		if (nextWeekNumber > windows.size()) {
			return; // 마지막 주차까지 생성됨(스터디 종료)
		}
		if (curriculum.weeks().stream().anyMatch(week -> week.weekNumber() == nextWeekNumber)) {
			return; // 멱등: 이미 다음 주차가 존재
		}
		CurriculumSprintWindow window = windows.get(nextWeekNumber - 1);

		// 입력 컨텍스트: 직전 주차 TODO + 리포트 + 멤버 회고(없으면 빈)
		List<String> priorTasks = repository.findWeeklyTasksByWeekId(currentWeekId).stream()
			.map(task -> task.title() + (task.required() ? " (필수)" : " (선택)"))
			.toList();
		String reportBody = repository.findLatestWeeklyReportBody(groupId).orElse("");
		List<String> retrospectives = repository.findCompletedRetrospectiveSummaries(currentWeekId);
		if (priorTasks.isEmpty() && reportBody.isBlank() && retrospectives.isEmpty()) {
			return; // 참고할 자료가 전혀 없으면 다음 주차 생성을 보류(다음 스케줄 틱에 재시도)
		}

		NextWeekPlanGeneration generation = generator.generate(new NextWeekPlanInput(
			nextWeekNumber, nextWeekNumber + "주차", "", reportBody, priorTasks, retrospectives
		));
		recorder.record(generation.response().toUsage(
			idGenerator.get(),
			actorUserId,
			groupId,
			LlmUsagePurpose.NEXT_WEEK_ADJUST,
			now
		));

		UUID weekId = idGenerator.get();
		List<WeeklyTask> tasks = toWeeklyTasks(weekId, generation.plan().tasks(), now);
		CurriculumWeek nextWeek = new CurriculumWeek(
			weekId,
			curriculum.id(),
			nextWeekNumber,
			nextWeekNumber + "주차",
			null,
			null,
			generation.plan().retrospectiveQuestions(),
			List.of(),
			List.of(),
			CurriculumWeekStatus.PENDING,
			window.startsAt(),
			window.endsAt(),
			tasks,
			now,
			now
		);
		repository.insertNextWeek(nextWeek);
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
