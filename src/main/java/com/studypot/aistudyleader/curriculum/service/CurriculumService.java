package com.studypot.aistudyleader.curriculum.service;

import com.studypot.aistudyleader.curriculum.domain.Curriculum;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGeneration;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGenerationRequest;
import com.studypot.aistudyleader.curriculum.domain.CurriculumSprintPlanner;
import com.studypot.aistudyleader.curriculum.domain.CurriculumSprintWindow;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStartContext;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeek;
import com.studypot.aistudyleader.curriculum.domain.CurrentLearningActivity;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgress;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgressStatus;
import com.studypot.aistudyleader.curriculum.domain.SubmittedAvailabilitySlot;
import com.studypot.aistudyleader.curriculum.domain.SubmittedOnboardingResponse;
import com.studypot.aistudyleader.curriculum.domain.TaskCompletion;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTask;
import com.studypot.aistudyleader.curriculum.repository.CurriculumPersistenceException;
import com.studypot.aistudyleader.curriculum.repository.CurriculumRepository;
import com.studypot.aistudyleader.llm.domain.LlmUsage;
import com.studypot.aistudyleader.llm.service.LlmCallFailure;
import com.studypot.aistudyleader.notification.service.NotificationEventPublisher;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class CurriculumService {

	private static final Logger log = LoggerFactory.getLogger(CurriculumService.class);

	private final CurriculumRepository repository;
	private final Supplier<CurriculumGenerator> generatorSupplier;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;
	private final NotificationEventPublisher notificationEvents;

	public CurriculumService(
		CurriculumRepository repository,
		CurriculumGenerator generator,
		Clock clock,
		Supplier<UUID> idGenerator
	) {
		this(repository, () -> Objects.requireNonNull(generator, "generator must not be null"), clock, idGenerator);
	}

	CurriculumService(
		CurriculumRepository repository,
		Supplier<CurriculumGenerator> generatorSupplier,
		Clock clock,
		Supplier<UUID> idGenerator
	) {
		this(repository, generatorSupplier, clock, idGenerator, NotificationEventPublisher.noop());
	}

	CurriculumService(
		CurriculumRepository repository,
		Supplier<CurriculumGenerator> generatorSupplier,
		Clock clock,
		Supplier<UUID> idGenerator,
		NotificationEventPublisher notificationEvents
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.generatorSupplier = Objects.requireNonNull(generatorSupplier, "generatorSupplier must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
		this.notificationEvents = Objects.requireNonNull(notificationEvents, "notificationEvents must not be null");
	}

	@Transactional(noRollbackFor = CurriculumGenerationException.class)
	public Curriculum startStudy(StartCurriculumCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		CurriculumStartContext context = requireStartContext(command.groupId(), command.authenticatedUserId());
		if (!context.isOwner()) {
			throw new CurriculumAccessDeniedException("only the study group owner can start the curriculum.");
		}
		if (context.groupStatus() != StudyGroupStatus.READY_TO_START) {
			throw new CurriculumStartRejectedException("study group must be READY_TO_START to start curriculum generation.");
		}
		if (!context.hasActiveMembership()) {
			throw new CurriculumStartRejectedException("owner onboarding must be submitted before starting the study.");
		}

		Instant now = clock.instant();
		List<SubmittedOnboardingResponse> submittedResponses = repository.findSubmittedOnboardingResponses(context.groupId());
		Map<String, Object> onboardingSummary = onboardingSummary(submittedResponses, now);
		List<CurriculumSprintWindow> sprintWindows = CurriculumSprintPlanner.fixedWeeklyWindows(context.startsAt(), context.endsAt());
		CurriculumGenerator generator = generatorSupplier.get();
		if (generator == null) {
			throw new CurriculumGenerationException("curriculum generator is not configured.");
		}
		CurriculumGeneration generation;
		try {
			generation = generator.generate(new CurriculumGenerationRequest(
				context,
				submittedResponses,
				onboardingSummary,
				now,
				sprintWindows
			));
		} catch (CurriculumGenerationException exception) {
			exception.failure().ifPresent(failure -> {
				try {
					saveFailedLlmUsage(failure, command.authenticatedUserId(), context.groupId(), now);
				} catch (RuntimeException saveException) {
					exception.addSuppressed(saveException);
				}
			});
			throw exception;
		}
		requireGenerationMatchesSprintWindows(generation, sprintWindows);

		UUID llmUsageId = idGenerator.get();
		UUID curriculumId = idGenerator.get();
		List<UUID> weekIds = generation.weeks().stream()
			.map(ignored -> idGenerator.get())
			.toList();
		List<UUID> taskIds = generation.weeks().stream()
			.flatMap(week -> week.tasks().stream())
			.map(ignored -> idGenerator.get())
			.toList();

		LlmUsage llmUsage = generation.toLlmUsage(llmUsageId, command.authenticatedUserId(), context.groupId(), now);
		Curriculum curriculum = generation.toCurriculum(
			curriculumId,
			context.groupId(),
			llmUsageId,
			onboardingSummary,
			now,
			sprintWindows,
			weekIds,
			taskIds
		);
		try {
			repository.saveStartedCurriculum(context.groupId(), now, llmUsage, curriculum);
		} catch (CurriculumPersistenceException exception) {
			throw new CurriculumStartRejectedException("curriculum start persistence failed.", exception);
		}
		publishStartedWeekNotification(curriculum);
		return curriculum;
	}

	private void requireGenerationMatchesSprintWindows(
		CurriculumGeneration generation,
		List<CurriculumSprintWindow> sprintWindows
	) {
		if (generation.weeks().size() != sprintWindows.size()) {
			throw new CurriculumGenerationException("generated curriculum weeks must match fixed weekly sprint windows.");
		}
	}

	private void publishStartedWeekNotification(Curriculum curriculum) {
		curriculum.weeks().stream()
			.min(Comparator.comparingInt(CurriculumWeek::weekNumber))
			.ifPresent(week -> publishNotification(() -> notificationEvents.publishWeekStarted(
				curriculum.groupId(),
				week.id(),
				week.weekNumber(),
				week.title()
			)));
	}

	private void saveFailedLlmUsage(LlmCallFailure failure, UUID userId, UUID groupId, Instant now) {
		LlmUsage llmUsage = failure.toUsage(idGenerator.get(), userId, groupId, now);
		repository.saveFailedLlmUsage(llmUsage);
	}

	@Transactional(readOnly = true)
	public Curriculum getCurriculum(GetCurriculumQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		CurriculumStartContext context = repository.findReadContext(query.groupId(), query.authenticatedUserId())
			.orElseGet(() -> {
				if (!repository.existsStudyGroup(query.groupId())) {
					throw new CurriculumGroupNotFoundException("study group was not found.");
				}
				throw new CurriculumAccessDeniedException("authenticated user is not a member of this study group.");
			});
		if (!context.canReadCurriculum()) {
			throw new CurriculumAccessDeniedException("active group membership is required to read the curriculum.");
		}
		return repository.findActiveCurriculumByGroupId(query.groupId())
			.orElseThrow(() -> new CurriculumNotFoundException("active curriculum was not found."));
	}

	@Transactional(readOnly = true)
	public CurriculumWeek getCurrentWeek(GetCurrentWeekQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		CurriculumStartContext context = repository.findReadContext(query.groupId(), query.authenticatedUserId())
			.orElseGet(() -> {
				if (!repository.existsStudyGroup(query.groupId())) {
					throw new CurriculumGroupNotFoundException("study group was not found.");
				}
				throw new CurriculumAccessDeniedException("authenticated user is not a member of this study group.");
			});
		if (!context.canReadCurriculum()) {
			throw new CurriculumAccessDeniedException("active group membership is required to read the current week.");
		}
		return repository.findCurrentWeekByGroupId(query.groupId())
			.orElseThrow(() -> new CurriculumNotFoundException("current curriculum week was not found."));
	}

	@Transactional(readOnly = true)
	public List<WeeklyTaskWithCompletion> listWeeklyTasks(ListWeeklyTasksQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		CurriculumStartContext context = repository.findReadContextByWeekId(query.weekId(), query.authenticatedUserId())
			.orElseGet(() -> {
				if (!repository.existsCurriculumWeek(query.weekId())) {
					throw new CurriculumNotFoundException("curriculum week was not found.");
				}
				throw new CurriculumAccessDeniedException("authenticated user is not a member of this study group.");
			});
		if (!context.canReadCurriculum()) {
			throw new CurriculumAccessDeniedException("active group membership is required to read weekly tasks.");
		}
		List<WeeklyTask> tasks = repository.findWeeklyTasksByWeekId(query.weekId());
		Map<UUID, TaskCompletion> completionsByTaskId = new LinkedHashMap<>();
		for (TaskCompletion completion : repository.findTaskCompletionsByWeekIdAndMemberId(query.weekId(), context.memberId())) {
			completionsByTaskId.put(completion.weeklyTaskId(), completion);
		}
		List<WeeklyTaskWithCompletion> result = new ArrayList<>(tasks.size());
		for (WeeklyTask task : tasks) {
			result.add(new WeeklyTaskWithCompletion(task, completionsByTaskId.get(task.id())));
		}
		return result;
	}

	@Transactional(readOnly = true)
	public CurrentLearningActivity getCurrentLearningActivity(GetLearningActivityQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		CurriculumStartContext context = repository.findReadContext(query.groupId(), query.authenticatedUserId())
			.orElseGet(() -> {
				if (!repository.existsStudyGroup(query.groupId())) {
					throw new CurriculumGroupNotFoundException("study group was not found.");
				}
				throw new CurriculumAccessDeniedException("authenticated user is not a member of this study group.");
			});
		if (!context.hasActiveMembership()) {
			throw new CurriculumAccessDeniedException("active group membership is required to read current learning activity.");
		}
		CurriculumWeek currentWeek = repository.findCurrentWeekByGroupId(query.groupId())
			.orElseThrow(() -> new CurriculumNotFoundException("current curriculum week was not found."));
		Optional<MemberWeekProgress> progress = repository.findMemberWeekProgress(currentWeek.id(), context.memberId());
		List<TaskCompletion> completions = repository.findTaskCompletionsByWeekIdAndMemberId(currentWeek.id(), context.memberId());
		return CurrentLearningActivity.of(query.groupId(), currentWeek, progress, completions);
	}

	@Transactional(readOnly = true)
	public MemberWeekProgress getMyWeekProgress(GetWeekProgressQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		CurriculumStartContext context = repository.findReadContextByWeekId(query.weekId(), query.authenticatedUserId())
			.orElseGet(() -> {
				if (!repository.existsCurriculumWeek(query.weekId())) {
					throw new CurriculumNotFoundException("curriculum week was not found.");
				}
				throw new CurriculumAccessDeniedException("authenticated user is not a member of this study group.");
			});
		if (!context.hasActiveMembership()) {
			throw new CurriculumAccessDeniedException("active group membership is required to read week progress.");
		}
		return repository.findMemberWeekProgress(query.weekId(), context.memberId())
			.orElseThrow(() -> new CurriculumNotFoundException("member week progress was not found."));
	}

	@Transactional
	public MemberWeekProgress updateMyWeekProgress(UpdateWeekProgressCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		CurriculumStartContext context = repository.findReadContextByWeekId(command.weekId(), command.authenticatedUserId())
			.orElseGet(() -> {
				if (!repository.existsCurriculumWeek(command.weekId())) {
					throw new CurriculumNotFoundException("curriculum week was not found.");
				}
				throw new CurriculumAccessDeniedException("authenticated user is not a member of this study group.");
			});
		if (!context.hasActiveMembership()) {
			throw new CurriculumAccessDeniedException("active group membership is required to update week progress.");
		}
		Instant now = clock.instant();
		return repository.findMemberWeekProgress(command.weekId(), context.memberId())
			.map(progress -> updateProgress(progress, command, now))
			.orElseGet(() -> createProgress(command, context.memberId(), now));
	}

	@Transactional
	public TaskCompletion completeMyTask(CompleteTaskCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		CurriculumStartContext context = repository.findReadContextByTaskId(command.taskId(), command.authenticatedUserId())
			.orElseGet(() -> {
				if (!repository.existsWeeklyTask(command.taskId())) {
					throw new CurriculumNotFoundException("weekly task was not found.");
				}
				throw new CurriculumAccessDeniedException("authenticated user is not a member of this study group.");
			});
		if (context.groupStatus() != StudyGroupStatus.ACTIVE || !context.hasActiveMembership()) {
			throw new CurriculumAccessDeniedException("active group membership is required to complete weekly tasks.");
		}
		WeeklyTask task = repository.findWeeklyTaskById(command.taskId())
			.orElseThrow(() -> new CurriculumNotFoundException("weekly task was not found."));
		Instant now = clock.instant();
		MemberWeekProgress progress = findOrCreateProgressForTaskCompletion(task.curriculumWeekId(), context.memberId(), now);
		return repository.findTaskCompletion(command.taskId(), context.memberId())
			.map(completion -> updateTaskCompletion(completion, command, now))
			.orElseGet(() -> createTaskCompletion(command, task, progress, context.memberId(), now));
	}

	private MemberWeekProgress findOrCreateProgressForTaskCompletion(UUID weekId, UUID memberId, Instant now) {
		return repository.findMemberWeekProgress(weekId, memberId)
			.orElseGet(() -> createProgressForTaskCompletion(weekId, memberId, now));
	}

	private MemberWeekProgress createProgressForTaskCompletion(UUID weekId, UUID memberId, Instant now) {
		Instant dueAt = repository.findWeekDueAt(weekId)
			.orElseThrow(() -> new CurriculumNotFoundException("curriculum week was not found."));
		MemberWeekProgress progress = MemberWeekProgress.create(
			idGenerator.get(),
			weekId,
			memberId,
			dueAt,
			MemberWeekProgressStatus.IN_PROGRESS,
			null,
			null,
			now
		);
		if (repository.insertMemberWeekProgress(progress)) {
			return progress;
		}
		MemberWeekProgress racedProgress = repository.findMemberWeekProgress(weekId, memberId)
			.orElseThrow(() -> new WeekProgressUpdateRejectedException("week progress could not be inserted."));
		return ensureProgressStartedForTaskCompletion(racedProgress, now);
	}

	private MemberWeekProgress ensureProgressStartedForTaskCompletion(MemberWeekProgress progress, Instant now) {
		if (progress.status() != MemberWeekProgressStatus.NOT_STARTED) {
			return progress;
		}
		MemberWeekProgress updated = applyProgressUpdate(progress, MemberWeekProgressStatus.IN_PROGRESS, null, null, now);
		if (!repository.updateMemberWeekProgress(updated)) {
			throw new WeekProgressUpdateRejectedException("week progress could not be updated after concurrent insert.");
		}
		return updated;
	}

	private MemberWeekProgress updateProgress(MemberWeekProgress progress, UpdateWeekProgressCommand command, Instant now) {
		MemberWeekProgress updated = applyProgressUpdate(progress, command, now);
		if (!repository.updateMemberWeekProgress(updated)) {
			throw new WeekProgressUpdateRejectedException("week progress could not be updated.");
		}
		return updated;
	}

	private TaskCompletion updateTaskCompletion(TaskCompletion completion, CompleteTaskCommand command, Instant now) {
		TaskCompletion updated = applyTaskCompletionUpdate(completion, command, now);
		if (!repository.updateTaskCompletion(updated)) {
			throw new TaskCompletionUpdateRejectedException("task completion could not be updated.");
		}
		return updated;
	}

	private TaskCompletion createTaskCompletion(
		CompleteTaskCommand command,
		WeeklyTask task,
		MemberWeekProgress progress,
		UUID memberId,
		Instant now
	) {
		TaskCompletion completion = createNewTaskCompletion(command, task, progress, memberId, now);
		if (repository.insertTaskCompletion(completion)) {
			return completion;
		}
		TaskCompletion racedCompletion = repository.findTaskCompletion(task.id(), memberId)
			.orElseThrow(() -> new TaskCompletionUpdateRejectedException("task completion could not be inserted."));
		TaskCompletion updated = applyTaskCompletionUpdate(racedCompletion, command, now);
		if (!repository.updateTaskCompletion(updated)) {
			throw new TaskCompletionUpdateRejectedException("task completion could not be updated after concurrent insert.");
		}
		return updated;
	}

	private MemberWeekProgress createProgress(UpdateWeekProgressCommand command, UUID memberId, Instant now) {
		Instant dueAt = repository.findWeekDueAt(command.weekId())
			.orElseThrow(() -> new CurriculumNotFoundException("curriculum week was not found."));
		MemberWeekProgress progress = applyProgressUpdate(
			MemberWeekProgress.create(
				idGenerator.get(),
				command.weekId(),
				memberId,
				dueAt,
				null,
				null,
				null,
				now
			),
			command,
			now
		);
		if (repository.insertMemberWeekProgress(progress)) {
			return progress;
		}
		MemberWeekProgress racedProgress = repository.findMemberWeekProgress(command.weekId(), memberId)
			.orElseThrow(() -> new WeekProgressUpdateRejectedException("week progress could not be inserted."));
		MemberWeekProgress updated = applyProgressUpdate(racedProgress, command, now);
		if (!repository.updateMemberWeekProgress(updated)) {
			throw new WeekProgressUpdateRejectedException("week progress could not be updated after concurrent insert.");
		}
		return updated;
	}

	private static MemberWeekProgress applyProgressUpdate(
		MemberWeekProgress progress,
		UpdateWeekProgressCommand command,
		Instant now
	) {
		return applyProgressUpdate(progress, command.status(), command.completionNote(), command.incompleteReason(), now);
	}

	private static MemberWeekProgress applyProgressUpdate(
		MemberWeekProgress progress,
		MemberWeekProgressStatus status,
		String completionNote,
		String incompleteReason,
		Instant now
	) {
		try {
			return progress.update(status, completionNote, incompleteReason, now);
		} catch (IllegalArgumentException exception) {
			throw new InvalidWeekProgressRequestException("status", exception.getMessage());
		}
	}

	private TaskCompletion createNewTaskCompletion(
		CompleteTaskCommand command,
		WeeklyTask task,
		MemberWeekProgress progress,
		UUID memberId,
		Instant now
	) {
		try {
			return TaskCompletion.create(
				idGenerator.get(),
				progress.id(),
				task.id(),
				memberId,
				task.dueAt(),
				command.status(),
				command.completionNote(),
				command.incompleteReason(),
				command.evidenceUrl(),
				now
			);
		} catch (IllegalArgumentException exception) {
			throw new InvalidTaskCompletionRequestException(taskCompletionField(exception.getMessage()), exception.getMessage());
		}
	}

	private static TaskCompletion applyTaskCompletionUpdate(
		TaskCompletion completion,
		CompleteTaskCommand command,
		Instant now
	) {
		try {
			return completion.update(command.status(), command.completionNote(), command.incompleteReason(), command.evidenceUrl(), now);
		} catch (IllegalArgumentException exception) {
			throw new InvalidTaskCompletionRequestException(taskCompletionField(exception.getMessage()), exception.getMessage());
		}
	}

	private static String taskCompletionField(String message) {
		if (message == null) {
			return "status";
		}
		String lowerMessage = message.toLowerCase(Locale.ROOT);
		if (lowerMessage.contains("incomplete reason")) {
			return "incompleteReason";
		}
		if (lowerMessage.contains("completion note")) {
			return "completionNote";
		}
		if (lowerMessage.contains("evidence url")) {
			return "evidenceUrl";
		}
		return "status";
	}

	private CurriculumStartContext requireStartContext(UUID groupId, UUID userId) {
		return repository.findStartContext(groupId, userId)
			.orElseGet(() -> {
				if (!repository.existsStudyGroup(groupId)) {
					throw new CurriculumGroupNotFoundException("study group was not found.");
				}
				throw new CurriculumAccessDeniedException("authenticated user is not a member of this study group.");
			});
	}

	private static void publishNotification(Runnable task) {
		try {
			task.run();
		} catch (RuntimeException exception) {
			log.warn("curriculum notification publishing failed", exception);
			// Notification creation must not roll back the primary curriculum command.
		}
	}

	private static Map<String, Object> onboardingSummary(List<SubmittedOnboardingResponse> responses, Instant generatedAt) {
		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("submittedResponseCount", responses.size());
		summary.put("generatedAt", generatedAt.toString());
		summary.put("keywordSkillLevels", scoreSummary(responses, ScoreKind.KEYWORD));
		summary.put("taskPreferences", scoreSummary(responses, ScoreKind.TASK));
		summary.put("availabilitySlots", availabilitySummary(responses));
		summary.put("additionalNoteCount", responses.stream().filter(response -> response.additionalNote() != null).count());
		return Map.copyOf(summary);
	}

	private static Map<String, Object> scoreSummary(List<SubmittedOnboardingResponse> responses, ScoreKind kind) {
		Map<String, List<Integer>> values = new LinkedHashMap<>();
		for (SubmittedOnboardingResponse response : responses) {
			Map<String, Integer> scores = kind == ScoreKind.KEYWORD ? response.keywordSkillLevels() : response.taskPreferences();
			for (var entry : scores.entrySet()) {
				values.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>()).add(entry.getValue());
			}
		}
		Map<String, Object> result = new LinkedHashMap<>();
		for (var entry : values.entrySet()) {
			List<Integer> scores = entry.getValue();
			double average = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
			result.put(entry.getKey(), Map.of(
				"average", average,
				"min", scores.stream().mapToInt(Integer::intValue).min().orElse(0),
				"max", scores.stream().mapToInt(Integer::intValue).max().orElse(0),
				"responses", scores.size()
			));
		}
		return result;
	}

	private static List<Map<String, Object>> availabilitySummary(List<SubmittedOnboardingResponse> responses) {
		Map<SlotKey, Integer> counts = new LinkedHashMap<>();
		for (SubmittedOnboardingResponse response : responses) {
			for (SubmittedAvailabilitySlot slot : response.availabilitySlots()) {
				SlotKey key = new SlotKey(slot.dayOfWeek(), slot.startTime(), slot.endTime(), slot.timezone());
				counts.merge(key, 1, Integer::sum);
			}
		}
		List<Map<String, Object>> result = new ArrayList<>();
		for (var entry : counts.entrySet()) {
			SlotKey slot = entry.getKey();
			result.add(Map.of(
				"dayOfWeek", slot.dayOfWeek(),
				"startTime", slot.startTime(),
				"endTime", slot.endTime(),
				"timezone", slot.timezone(),
				"responses", entry.getValue()
			));
		}
		return List.copyOf(result);
	}

	private enum ScoreKind {
		KEYWORD,
		TASK
	}

	private record SlotKey(int dayOfWeek, String startTime, String endTime, String timezone) {
	}
}
