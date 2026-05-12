package com.studypot.aistudyleader.retrospective.service;

import com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus;
import com.studypot.aistudyleader.retrospective.domain.Retrospective;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveMembershipContext;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveProgress;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveTaskSummary;
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
import org.springframework.transaction.annotation.Transactional;

public class RetrospectiveService {

	private static final String CONVERSATION_SUMMARY_SOURCE_PLACEHOLDER = "RETROSPECTIVE_CONVERSATION_PENDING";

	private final RetrospectiveRepository repository;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;

	public RetrospectiveService(RetrospectiveRepository repository, Clock clock, Supplier<UUID> idGenerator) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
	}

	@Transactional
	public Retrospective requestMyRetrospective(RequestRetrospectiveCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		RetrospectiveMembershipContext context = requireMembership(command.weekId(), command.authenticatedUserId());
		if (!context.canRequestRetrospective()) {
			throw new RetrospectiveAccessDeniedException("active group membership is required to request retrospective.");
		}
		RetrospectiveProgress progress = requireProgress(command.weekId(), context.memberId());
		return repository.findRetrospective(progress.id(), command.weekId(), context.memberId())
			.orElseGet(() -> createRetrospective(command, context, progress));
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

	private Retrospective createRetrospective(
		RequestRetrospectiveCommand command,
		RetrospectiveMembershipContext context,
		RetrospectiveProgress progress
	) {
		Instant now = clock.instant();
		List<RetrospectiveTaskSummary> taskSummaries = repository.findTaskSummaries(progress.id(), command.weekId(), context.memberId());
		Retrospective retrospective = Retrospective.requested(
			idGenerator.get(),
			progress.id(),
			command.weekId(),
			context.memberId(),
			command.triggerType(),
			inputSummary(progress, taskSummaries, now),
			now
		);
		if (!repository.insertRetrospective(retrospective)) {
			throw new RetrospectiveMutationRejectedException("retrospective could not be inserted.");
		}
		return retrospective;
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

	private static Map<String, Object> inputSummary(
		RetrospectiveProgress progress,
		List<RetrospectiveTaskSummary> taskSummaries,
		Instant generatedAt
	) {
		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("generatedAt", generatedAt.toString());
		summary.put("progress", progressSummary(progress));
		summary.put("taskCompletionCounts", taskCompletionCounts(taskSummaries));
		summary.put("tasks", taskDetails(taskSummaries));
		summary.put("conversationSummary", Map.of(
			"status", "NOT_AVAILABLE",
			"source", CONVERSATION_SUMMARY_SOURCE_PLACEHOLDER
		));
		return summary;
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
}
