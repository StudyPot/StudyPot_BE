package com.studypot.aistudyleader.curriculum.service;

import com.studypot.aistudyleader.curriculum.domain.Curriculum;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGeneration;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGenerationRequest;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStartContext;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeek;
import com.studypot.aistudyleader.curriculum.domain.LlmUsage;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgress;
import com.studypot.aistudyleader.curriculum.domain.SubmittedAvailabilitySlot;
import com.studypot.aistudyleader.curriculum.domain.SubmittedOnboardingResponse;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTask;
import com.studypot.aistudyleader.curriculum.repository.CurriculumPersistenceException;
import com.studypot.aistudyleader.curriculum.repository.CurriculumRepository;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
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

public class CurriculumService {

	private final CurriculumRepository repository;
	private final CurriculumGenerator generator;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;

	public CurriculumService(
		CurriculumRepository repository,
		CurriculumGenerator generator,
		Clock clock,
		Supplier<UUID> idGenerator
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.generator = Objects.requireNonNull(generator, "generator must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
	}

	@Transactional
	public Curriculum startStudy(StartCurriculumCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		CurriculumStartContext context = requireStartContext(command.groupId(), command.authenticatedUserId());
		if (!context.isOwner()) {
			throw new CurriculumAccessDeniedException("only the study group owner can start the curriculum.");
		}
		if (context.groupStatus() != StudyGroupStatus.ONBOARDING) {
			throw new CurriculumStartRejectedException("study group must be ONBOARDING to start curriculum generation.");
		}
		if (!context.hasActiveMembership()) {
			throw new CurriculumStartRejectedException("owner onboarding must be submitted before starting the study.");
		}

		Instant now = clock.instant();
		List<SubmittedOnboardingResponse> submittedResponses = repository.findSubmittedOnboardingResponses(context.groupId());
		Map<String, Object> onboardingSummary = onboardingSummary(submittedResponses, now);
		CurriculumGeneration generation = generator.generate(new CurriculumGenerationRequest(
			context,
			submittedResponses,
			onboardingSummary,
			now
		));

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
			weekIds,
			taskIds
		);
		try {
			repository.saveStartedCurriculum(context.groupId(), now, llmUsage, curriculum);
		} catch (CurriculumPersistenceException exception) {
			throw new CurriculumStartRejectedException("curriculum start persistence failed.", exception);
		}
		return curriculum;
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
	public List<WeeklyTask> listWeeklyTasks(ListWeeklyTasksQuery query) {
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
		return repository.findWeeklyTasksByWeekId(query.weekId());
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

	private MemberWeekProgress updateProgress(MemberWeekProgress progress, UpdateWeekProgressCommand command, Instant now) {
		MemberWeekProgress updated = applyProgressUpdate(progress, command, now);
		if (!repository.updateMemberWeekProgress(updated)) {
			throw new WeekProgressUpdateRejectedException("week progress could not be updated.");
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
		try {
			return progress.update(command.status(), command.completionNote(), command.incompleteReason(), now);
		} catch (IllegalArgumentException exception) {
			throw new InvalidWeekProgressRequestException("status", exception.getMessage());
		}
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
