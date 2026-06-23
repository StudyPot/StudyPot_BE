package com.studypot.aistudyleader.retrospective.service;

import com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.service.LlmCallFailure;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import com.studypot.aistudyleader.notification.service.NotificationEventPublisher;
import com.studypot.aistudyleader.retrospective.domain.Retrospective;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveAiContext;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveMembershipContext;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveProgress;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveTaskSummary;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveTriggerType;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveWeekOverview;
import com.studypot.aistudyleader.retrospective.repository.RetrospectiveRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class RetrospectiveService {

	private static final String CONVERSATION_SUMMARY_SOURCE_PLACEHOLDER = "RETROSPECTIVE_CONVERSATION_PENDING";
	private static final Logger log = LoggerFactory.getLogger(RetrospectiveService.class);

	private final RetrospectiveRepository repository;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;
	private final RetrospectiveFeedbackGenerator feedbackGenerator;
	private final LlmUsageRecorder usageRecorder;
	private final NotificationEventPublisher notificationEvents;

	public RetrospectiveService(RetrospectiveRepository repository, Clock clock, Supplier<UUID> idGenerator) {
		this(repository, clock, idGenerator, null, null, NotificationEventPublisher.noop());
	}

	public RetrospectiveService(
		RetrospectiveRepository repository,
		Clock clock,
		Supplier<UUID> idGenerator,
		RetrospectiveFeedbackGenerator feedbackGenerator,
		LlmUsageRecorder usageRecorder
	) {
		this(
			repository,
			clock,
			idGenerator,
			feedbackGenerator,
			usageRecorder,
			NotificationEventPublisher.noop()
		);
	}

	public RetrospectiveService(
		RetrospectiveRepository repository,
		Clock clock,
		Supplier<UUID> idGenerator,
		RetrospectiveFeedbackGenerator feedbackGenerator,
		LlmUsageRecorder usageRecorder,
		NotificationEventPublisher notificationEvents
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
		if ((feedbackGenerator == null) != (usageRecorder == null)) {
			throw new IllegalArgumentException("feedbackGenerator and usageRecorder must be configured together.");
		}
		this.feedbackGenerator = feedbackGenerator;
		this.usageRecorder = usageRecorder;
		this.notificationEvents = Objects.requireNonNull(notificationEvents, "notificationEvents must not be null");
	}

	@Transactional
	public Retrospective requestMyRetrospective(RequestRetrospectiveCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		RetrospectiveMembershipContext context = requireMembership(command.weekId(), command.authenticatedUserId());
		if (!context.canRequestRetrospective()) {
			throw new RetrospectiveAccessDeniedException("active group membership is required to request retrospective.");
		}
		RetrospectiveProgress progress = requireProgress(command.weekId(), context.memberId());
		Retrospective retrospective = repository.findRetrospective(progress.id(), command.weekId(), context.memberId())
			.orElseGet(() -> createRetrospective(command, context, progress));
		return generateFeedbackIfConfigured(command.authenticatedUserId(), context.groupId(), retrospective);
	}

	@Transactional(readOnly = true)
	public Retrospective getMyRetrospective(GetMyRetrospectiveQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		RetrospectiveMembershipContext context = requireMembership(query.weekId(), query.authenticatedUserId());
		if (!context.hasActiveMembership()) {
			throw new RetrospectiveAccessDeniedException("active group membership is required to read retrospective.");
		}
		RetrospectiveProgress progress = requireProgress(query.weekId(), context.memberId());
		return repository.findRetrospective(progress.id(), query.weekId(), context.memberId())
			.orElseThrow(() -> new RetrospectiveNotFoundException("retrospective was not found."));
	}

	@Transactional(readOnly = true)
	public List<Retrospective> listMyRetrospectives(ListMyRetrospectivesQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		RetrospectiveMembershipContext context = repository.findMembershipByGroupId(query.groupId(), query.authenticatedUserId())
			.orElseThrow(() -> new RetrospectiveAccessDeniedException("authenticated user is not a member of this study group."));
		if (!context.hasActiveMembership()) {
			throw new RetrospectiveAccessDeniedException("active group membership is required to read retrospectives.");
		}
		return repository.findMyRetrospectivesByGroup(query.groupId(), context.memberId());
	}

	@Transactional(readOnly = true)
	public List<RetrospectiveWeekOverview> getRetrospectiveOverview(GetRetrospectiveOverviewQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		RetrospectiveMembershipContext context = repository.findMembershipByGroupId(query.groupId(), query.authenticatedUserId())
			.orElseThrow(() -> new RetrospectiveAccessDeniedException("authenticated user is not a member of this study group."));
		if (!context.hasActiveMembership()) {
			throw new RetrospectiveAccessDeniedException("active group membership is required to read retrospectives.");
		}
		return repository.findRetrospectiveOverview(query.groupId(), context.memberId());
	}

	/**
	 * 멤버가 회고 설문에 답한 결과를 저장한다(AI 피드백 없음). 그 주차 필수 TODO 를 모두 완료해야만 가능(잠금).
	 */
	@Transactional
	public Retrospective submitMyRetrospective(SubmitRetrospectiveCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		RetrospectiveMembershipContext context = requireMembership(command.weekId(), command.authenticatedUserId());
		if (!context.canRequestRetrospective()) {
			throw new RetrospectiveAccessDeniedException("active group membership is required to submit retrospective.");
		}
		RetrospectiveProgress progress = requireProgress(command.weekId(), context.memberId());
		if (!allRequiredTasksDone(progress.id(), command.weekId(), context.memberId())) {
			throw new RetrospectiveMutationRejectedException("retrospective is locked until all required tasks are completed.");
		}
		Instant now = clock.instant();
		Map<String, Object> inputSummary = Map.of(
			"answers", command.answers(),
			"submittedAt", now.toString()
		);
		return repository.findRetrospective(progress.id(), command.weekId(), context.memberId())
			.map(existing -> {
				Retrospective updated = existing.withAnswers(inputSummary, now);
				if (!repository.updateRetrospectiveAnswers(updated)) {
					throw new RetrospectiveMutationRejectedException("retrospective answers could not be updated.");
				}
				return updated;
			})
			.orElseGet(() -> {
				Retrospective created = Retrospective.answered(
					idGenerator.get(),
					progress.id(),
					command.weekId(),
					context.memberId(),
					RetrospectiveTriggerType.MANUAL,
					inputSummary,
					now
				);
				if (!repository.insertRetrospective(created)) {
					throw new RetrospectiveMutationRejectedException("retrospective answers could not be saved.");
				}
				return created;
			});
	}

	private boolean allRequiredTasksDone(UUID progressId, UUID weekId, UUID memberId) {
		List<RetrospectiveTaskSummary> summaries = repository.findTaskSummaries(progressId, weekId, memberId);
		for (RetrospectiveTaskSummary summary : summaries) {
			if (summary.required() && summary.status() != TaskCompletionStatus.DONE) {
				return false;
			}
		}
		return true;
	}

	@Transactional
	Retrospective applyFeedback(ApplyRetrospectiveFeedbackCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		Retrospective retrospective = requireRetrospective(command.retrospectiveId());
		Retrospective completed = retrospective.completeWithFeedback(command.llmUsageId(), command.feedbackResult(), clock.instant());
		if (!repository.updateRetrospectiveResult(completed)) {
			throw new RetrospectiveMutationRejectedException("retrospective feedback could not be updated.");
		}
		return completed;
	}

	@Transactional
	Retrospective failFeedback(FailRetrospectiveFeedbackCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		Retrospective retrospective = requireRetrospective(command.retrospectiveId());
		Retrospective failed = retrospective.failFeedback(command.llmUsageId(), command.errorCode(), command.errorMessage(), clock.instant());
		if (!repository.updateRetrospectiveResult(failed)) {
			throw new RetrospectiveMutationRejectedException("retrospective feedback failure could not be updated.");
		}
		return failed;
	}

	private Retrospective createRetrospective(
		RequestRetrospectiveCommand command,
		RetrospectiveMembershipContext context,
		RetrospectiveProgress progress
	) {
		Instant now = clock.instant();
		UUID retrospectiveId = idGenerator.get();
		List<RetrospectiveTaskSummary> taskSummaries = repository.findTaskSummaries(progress.id(), command.weekId(), context.memberId());
		RetrospectiveAiContext aiContext = repository.findAiContext(context.groupId(), context.memberId(), command.weekId(), retrospectiveId);
		Retrospective retrospective = Retrospective.requested(
			retrospectiveId,
			progress.id(),
			command.weekId(),
			context.memberId(),
			command.triggerType(),
			inputSummary(progress, taskSummaries, aiContext, now),
			now
		);
		if (!repository.insertRetrospective(retrospective)) {
			throw new RetrospectiveMutationRejectedException("retrospective could not be inserted.");
		}
		return retrospective;
	}

	private Retrospective generateFeedbackIfConfigured(UUID userId, UUID groupId, Retrospective retrospective) {
		if (feedbackGenerator == null) {
			return retrospective;
		}
		if (retrospective.status().isCompleted() || retrospective.status().isProcessing()) {
			return retrospective;
		}
		Retrospective processing = retrospective.startProcessing(clock.instant());
		updateRetrospective(processing, "retrospective feedback processing state could not be updated.");
		try {
			RetrospectiveFeedbackGeneration generation = feedbackGenerator.generate(processing);
			UUID llmUsageId = idGenerator.get();
			Instant now = clock.instant();
			usageRecorder.record(generation.response().toUsage(
				llmUsageId,
				userId,
				groupId,
				LlmUsagePurpose.RETROSPECTIVE_FEEDBACK,
				now
			));
			Retrospective completed = processing.completeWithFeedback(llmUsageId, generation.feedbackResult(), now);
			updateRetrospective(completed, "retrospective feedback could not be updated.");
			publishCompletedRetrospectiveNotifications(userId, groupId, completed);
			return completed;
		} catch (RetrospectiveFeedbackGenerationException exception) {
			return failGeneratedFeedback(userId, groupId, processing, exception);
		}
	}

	private void publishCompletedRetrospectiveNotifications(UUID userId, UUID groupId, Retrospective completed) {
		publishNotification(() -> notificationEvents.publishRetrospectiveReady(
			groupId,
			userId,
			completed.id(),
			completed.curriculumWeekId()
		));
		if (!completed.nextWeekAdjustment().isEmpty()) {
			publishNotification(() -> notificationEvents.publishNextWeekAdjusted(
				groupId,
				userId,
				completed.id(),
				completed.curriculumWeekId()
			));
		}
	}

	private Retrospective failGeneratedFeedback(
		UUID userId,
		UUID groupId,
		Retrospective processing,
		RetrospectiveFeedbackGenerationException exception
	) {
		LlmCallFailure failure = exception.failure();
		UUID llmUsageId = idGenerator.get();
		Instant now = clock.instant();
		usageRecorder.record(failure.toUsage(llmUsageId, userId, groupId, now));
		Retrospective failed = processing.failFeedback(llmUsageId, failure.errorCode(), failureMessage(failure, exception), now);
		updateRetrospective(failed, "retrospective feedback failure could not be updated.");
		return failed;
	}

	private void updateRetrospective(Retrospective retrospective, String failureMessage) {
		if (!repository.updateRetrospectiveResult(retrospective)) {
			throw new RetrospectiveMutationRejectedException(failureMessage);
		}
	}

	private RetrospectiveMembershipContext requireMembership(UUID weekId, UUID userId) {
		return repository.findMembershipByWeekId(weekId, userId)
			.orElseGet(() -> {
				if (!repository.existsCurriculumWeek(weekId)) {
					throw new RetrospectiveNotFoundException("curriculum week was not found.");
				}
				throw new RetrospectiveAccessDeniedException("authenticated user is not a member of this study group.");
			});
	}

	private RetrospectiveProgress requireProgress(UUID weekId, UUID memberId) {
		return repository.findProgress(weekId, memberId)
			.orElseThrow(() -> new RetrospectiveNotFoundException("member week progress was not found."));
	}

	private Retrospective requireRetrospective(UUID retrospectiveId) {
		return repository.findRetrospectiveById(retrospectiveId)
			.orElseThrow(() -> new RetrospectiveNotFoundException("retrospective was not found."));
	}

	private static Map<String, Object> inputSummary(
		RetrospectiveProgress progress,
		List<RetrospectiveTaskSummary> taskSummaries,
		RetrospectiveAiContext aiContext,
		Instant generatedAt
	) {
		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("generatedAt", generatedAt.toString());
		summary.put("progress", progressSummary(progress));
		summary.put("taskCompletionCounts", taskCompletionCounts(taskSummaries));
		summary.put("tasks", taskDetails(taskSummaries));
		summary.put("onboarding", aiContext.onboarding());
		summary.put("rules", aiContext.rules());
		summary.put("ruleViolations", aiContext.ruleViolations());
		summary.put("priorRetrospectives", aiContext.priorRetrospectives());
		summary.put("conversationSummary", conversationSummary(aiContext));
		return summary;
	}

	private static Map<String, Object> conversationSummary(RetrospectiveAiContext aiContext) {
		Map<String, Object> summary = aiContext.conversationSummary();
		if (summary != null && !summary.isEmpty()) {
			return summary;
		}
		return Map.of(
			"status", "NOT_AVAILABLE",
			"source", CONVERSATION_SUMMARY_SOURCE_PLACEHOLDER
		);
	}

	private static Map<String, Object> progressSummary(RetrospectiveProgress progress) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", progress.id().toString());
		result.put("status", progress.status().name());
		putInstant(result, "startedAt", progress.startedAt());
		putInstant(result, "dueAt", progress.dueAt());
		putInstant(result, "completedAt", progress.completedAt());
		putText(result, "completionNote", progress.completionNote());
		putText(result, "incompleteReason", progress.incompleteReason());
		putInstant(result, "reasonSubmittedAt", progress.reasonSubmittedAt());
		return result;
	}

	private static Map<String, Integer> taskCompletionCounts(List<RetrospectiveTaskSummary> taskSummaries) {
		Map<String, Integer> result = new LinkedHashMap<>();
		for (TaskCompletionStatus status : TaskCompletionStatus.values()) {
			result.put(status.name(), 0);
		}
		for (RetrospectiveTaskSummary taskSummary : taskSummaries) {
			result.computeIfPresent(taskSummary.status().name(), (ignored, count) -> count + 1);
		}
		return result;
	}

	private static List<Map<String, Object>> taskDetails(List<RetrospectiveTaskSummary> taskSummaries) {
		List<Map<String, Object>> result = new ArrayList<>();
		for (RetrospectiveTaskSummary taskSummary : taskSummaries) {
			Map<String, Object> task = new LinkedHashMap<>();
			task.put("taskId", taskSummary.taskId().toString());
			task.put("displayOrder", taskSummary.displayOrder());
			task.put("taskType", taskSummary.taskType().name());
			task.put("title", taskSummary.title());
			task.put("required", taskSummary.required());
			putInstant(task, "dueAt", taskSummary.dueAt());
			task.put("status", taskSummary.status().name());
			putInstant(task, "completedAt", taskSummary.completedAt());
			putText(task, "completionNote", taskSummary.completionNote());
			putText(task, "incompleteReason", taskSummary.incompleteReason());
			putInstant(task, "reasonSubmittedAt", taskSummary.reasonSubmittedAt());
			result.add(task);
		}
		return List.copyOf(result);
	}

	private static void putText(Map<String, Object> target, String key, String value) {
		if (value != null) {
			target.put(key, value);
		}
	}

	private static void putInstant(Map<String, Object> target, String key, Instant value) {
		if (value != null) {
			target.put(key, value.toString());
		}
	}

	private static void publishNotification(Runnable task) {
		try {
			task.run();
		} catch (RuntimeException exception) {
			log.warn("retrospective notification publishing failed", exception);
			// Notification creation must not roll back the primary retrospective command.
		}
	}

	private static String failureMessage(LlmCallFailure failure, RuntimeException exception) {
		if (failure.responseSummary() != null && !failure.responseSummary().isBlank()) {
			return failure.responseSummary();
		}
		return exception.getMessage();
	}
}
