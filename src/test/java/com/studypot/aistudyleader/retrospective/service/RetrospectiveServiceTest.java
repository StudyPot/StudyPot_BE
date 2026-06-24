package com.studypot.aistudyleader.retrospective.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgressStatus;
import com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsage;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.service.LlmCallFailure;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import com.studypot.aistudyleader.notification.service.NotificationEventPublisher;
import com.studypot.aistudyleader.retrospective.domain.Retrospective;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveAiContext;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveFeedbackResult;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveMembershipContext;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveProgress;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveStatus;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveTaskSummary;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveTriggerType;
import com.studypot.aistudyleader.retrospective.repository.RetrospectiveRepository;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RetrospectiveServiceTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000006101");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000006102");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000006103");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000006104");
	private static final UUID PROGRESS_ID = UUID.fromString("018f0000-0000-7000-8000-000000006105");
	private static final UUID RETROSPECTIVE_ID = UUID.fromString("018f0000-0000-7000-8000-000000006106");
	private static final UUID READING_TASK_ID = UUID.fromString("018f0000-0000-7000-8000-000000006107");
	private static final UUID PRACTICE_TASK_ID = UUID.fromString("018f0000-0000-7000-8000-000000006108");
	private static final UUID LLM_USAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000006109");
	private static final Instant NOW = Instant.parse("2026-05-12T02:45:00Z");
	private static final Instant WEEK_DUE_AT = Instant.parse("2026-05-18T02:45:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Test
	void requestCreatesPendingRetrospectiveFromExistingProgressAndTaskContext() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.membership = activeMember();
		repository.progress = completedProgress();
		repository.taskSummaries = List.of(
			taskSummary(READING_TASK_ID, WeeklyTaskType.READING, TaskCompletionStatus.DONE, "정리 완료", null),
			taskSummary(PRACTICE_TASK_ID, WeeklyTaskType.PRACTICE, TaskCompletionStatus.INCOMPLETE, null, "실습 시간 부족")
		);
		RetrospectiveService service = service(repository, RETROSPECTIVE_ID);

		Retrospective result = service.requestMyRetrospective(new RequestRetrospectiveCommand(
			USER_ID,
			WEEK_ID,
			RetrospectiveTriggerType.MANUAL
		));

		assertThat(result.id()).isEqualTo(RETROSPECTIVE_ID);
		assertThat(result.progressId()).isEqualTo(PROGRESS_ID);
		assertThat(result.memberId()).isEqualTo(MEMBER_ID);
		assertThat(result.triggerType()).isEqualTo(RetrospectiveTriggerType.MANUAL);
		assertThat(result.status()).isEqualTo(RetrospectiveStatus.PENDING);
		assertThat(result.requestedAt()).isEqualTo(NOW);
		assertThat(result.completedAt()).isNull();
		assertThat(repository.insertedRetrospective).isSameAs(result);
		assertThat(result.inputSummary())
			.containsEntry("generatedAt", NOW.toString())
			.containsKey("progress")
			.containsKey("taskCompletionCounts")
			.containsKey("tasks")
			.containsKey("conversationSummary");
		@SuppressWarnings("unchecked")
		Map<String, Integer> counts = (Map<String, Integer>) result.inputSummary().get("taskCompletionCounts");
		assertThat(counts)
			.containsEntry("DONE", 1)
			.containsEntry("INCOMPLETE", 1)
			.containsEntry("TODO", 0)
			.containsEntry("SKIPPED", 0);
		assertThat(result.aiFeedback()).isEmpty();
		assertThat(result.nextWeekAdjustment()).isEmpty();
	}

	@Test
	void requestGeneratesAiFeedbackFromDbFirstContextAndRecordsSuccessfulUsage() {
		CapturingRepository repository = readyRepository();
		repository.aiContext = aiContext();
		CapturingUsageRecorder usageRecorder = new CapturingUsageRecorder();
		RetrospectiveFeedbackResult feedbackResult = RetrospectiveFeedbackResult.of(
			"미완료 사유가 실습 시간 부족에 집중되어 있습니다.",
			List.of("읽기 과제 정리가 명확합니다."),
			List.of("실습 과제가 밀렸습니다."),
			List.of("다음 주 실습량을 낮춥니다."),
			Map.of("difficulty", "slightly_lower", "taskChanges", List.of("필수 실습 1개 분리"))
		);
		CapturingFeedbackGenerator generator = new CapturingFeedbackGenerator(new RetrospectiveFeedbackGeneration(
			feedbackResult,
			successfulResponse()
		));
		RetrospectiveService service = service(repository, generator, usageRecorder, RETROSPECTIVE_ID, LLM_USAGE_ID);

		Retrospective result = service.requestMyRetrospective(new RequestRetrospectiveCommand(
			USER_ID,
			WEEK_ID,
			RetrospectiveTriggerType.MANUAL
		));

		assertThat(result.status()).isEqualTo(RetrospectiveStatus.COMPLETED);
		assertThat(result.llmUsageId()).isEqualTo(LLM_USAGE_ID);
		assertThat(result.aiFeedback()).containsEntry("summary", "미완료 사유가 실습 시간 부족에 집중되어 있습니다.");
		assertThat(result.nextWeekAdjustment()).containsEntry("difficulty", "slightly_lower");
		assertThat(repository.updatedStatuses()).containsExactly(RetrospectiveStatus.PROCESSING, RetrospectiveStatus.COMPLETED);
		assertThat(generator.inputSummary)
			.containsKey("progress")
			.containsKey("tasks")
			.containsKey("onboarding")
			.containsKey("rules")
			.containsKey("ruleViolations")
			.containsKey("priorRetrospectives")
			.containsKey("conversationSummary");
		assertThat(usageRecorder.usage.id()).isEqualTo(LLM_USAGE_ID);
		assertThat(usageRecorder.usage.userId()).isEqualTo(USER_ID);
		assertThat(usageRecorder.usage.groupId()).isEqualTo(GROUP_ID);
		assertThat(usageRecorder.usage.purpose()).isEqualTo(LlmUsagePurpose.RETROSPECTIVE_FEEDBACK);
		assertThat(usageRecorder.usage.status()).isEqualTo(LlmUsageStatus.SUCCESS);
		assertThat(usageRecorder.usage.requestPayload())
			.containsEntry("purpose", "RETROSPECTIVE_FEEDBACK")
			.containsEntry("retrospectiveId", RETROSPECTIVE_ID.toString())
			.containsEntry("taskCount", 2)
			.containsEntry("ruleViolationCount", 1);
	}

	@Test
	void requestPublishesRetrospectiveReadyAndNextWeekAdjustmentNotifications() {
		CapturingRepository repository = readyRepository();
		CapturingUsageRecorder usageRecorder = new CapturingUsageRecorder();
		CapturingNotificationPublisher notifications = new CapturingNotificationPublisher();
		RetrospectiveFeedbackResult feedbackResult = RetrospectiveFeedbackResult.of(
			"미완료 사유가 실습 시간 부족에 집중되어 있습니다.",
			List.of("읽기 과제 정리가 명확합니다."),
			List.of("실습 과제가 밀렸습니다."),
			List.of("다음 주 실습량을 낮춥니다."),
			Map.of("difficulty", "slightly_lower")
		);
		RetrospectiveService service = service(
			repository,
			new CapturingFeedbackGenerator(new RetrospectiveFeedbackGeneration(feedbackResult, successfulResponse())),
			usageRecorder,
			notifications,
			RETROSPECTIVE_ID,
			LLM_USAGE_ID
		);

		service.requestMyRetrospective(new RequestRetrospectiveCommand(
			USER_ID,
			WEEK_ID,
			RetrospectiveTriggerType.MANUAL
		));

		RetrospectiveNotice expected = new RetrospectiveNotice(GROUP_ID, USER_ID, RETROSPECTIVE_ID, WEEK_ID);
		assertThat(notifications.retrospectiveReady).containsExactly(expected);
		assertThat(notifications.nextWeekAdjusted).containsExactly(expected);
	}

	@Test
	void requestMarksFailedAndRecordsFailedUsageWhenProviderFails() {
		CapturingRepository repository = readyRepository();
		CapturingUsageRecorder usageRecorder = new CapturingUsageRecorder();
		LlmCallFailure failure = new LlmCallFailure(
			LlmUsagePurpose.RETROSPECTIVE_FEEDBACK,
			LlmProvider.OPENAI,
			"gpt-4o-mini",
			12,
			0,
			BigDecimal.ZERO,
			500,
			LlmUsageStatus.FAILED,
			"OPENAI_REQUEST_FAILED",
			Map.of("purpose", "RETROSPECTIVE_FEEDBACK"),
			"OpenAI request failed."
		);
		RetrospectiveService service = service(
			repository,
			new FailingFeedbackGenerator(failure),
			usageRecorder,
			RETROSPECTIVE_ID,
			LLM_USAGE_ID
		);

		Retrospective result = service.requestMyRetrospective(new RequestRetrospectiveCommand(
			USER_ID,
			WEEK_ID,
			RetrospectiveTriggerType.MANUAL
		));

		assertThat(result.status()).isEqualTo(RetrospectiveStatus.FAILED);
		assertThat(result.llmUsageId()).isEqualTo(LLM_USAGE_ID);
		assertThat(result.nextWeekAdjustment()).isEmpty();
		assertThat(result.aiFeedback()).containsKey("error");
		assertThat(repository.updatedStatuses()).containsExactly(RetrospectiveStatus.PROCESSING, RetrospectiveStatus.FAILED);
		assertThat(usageRecorder.usage.status()).isEqualTo(LlmUsageStatus.FAILED);
		assertThat(usageRecorder.usage.errorCode()).isEqualTo("OPENAI_REQUEST_FAILED");
	}

	@Test
	void requestRetriesFailedRetrospectiveWithoutCreatingDuplicate() {
		Retrospective failedExisting = existingRetrospective(RetrospectiveStatus.FAILED);
		CapturingRepository repository = readyRepository();
		repository.existingRetrospective = failedExisting;
		CapturingUsageRecorder usageRecorder = new CapturingUsageRecorder();
		RetrospectiveFeedbackResult feedbackResult = RetrospectiveFeedbackResult.of(
			"재시도 후 피드백이 생성되었습니다.",
			List.of(),
			List.of(),
			List.of(),
			Map.of()
		);
		RetrospectiveService service = service(
			repository,
			new CapturingFeedbackGenerator(new RetrospectiveFeedbackGeneration(feedbackResult, successfulResponse())),
			usageRecorder,
			LLM_USAGE_ID
		);

		Retrospective result = service.requestMyRetrospective(new RequestRetrospectiveCommand(
			USER_ID,
			WEEK_ID,
			RetrospectiveTriggerType.MANUAL
		));

		assertThat(result.status()).isEqualTo(RetrospectiveStatus.COMPLETED);
		assertThat(repository.insertedRetrospective).isNull();
		assertThat(repository.updatedStatuses()).containsExactly(RetrospectiveStatus.PROCESSING, RetrospectiveStatus.COMPLETED);
	}

	@Test
	void requestReturnsExistingRetrospectiveWithoutDuplicateInsert() {
		Retrospective existing = existingRetrospective(RetrospectiveStatus.PROCESSING);
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.membership = activeMember();
		repository.progress = completedProgress();
		repository.existingRetrospective = existing;
		RetrospectiveService service = service(repository, RETROSPECTIVE_ID);

		Retrospective result = service.requestMyRetrospective(new RequestRetrospectiveCommand(
			USER_ID,
			WEEK_ID,
			RetrospectiveTriggerType.WEEK_ENDED
		));

		assertThat(result).isSameAs(existing);
		assertThat(repository.insertedRetrospective).isNull();
	}

	@Test
	void getReturnsExistingOwnRetrospectiveForActiveMember() {
		Retrospective existing = existingRetrospective(RetrospectiveStatus.COMPLETED);
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.membership = activeMember();
		repository.progress = completedProgress();
		repository.existingRetrospective = existing;
		RetrospectiveService service = service(repository);

		Retrospective result = service.getMyRetrospective(new GetMyRetrospectiveQuery(USER_ID, WEEK_ID));

		assertThat(result).isSameAs(existing);
	}

	@Test
	void listMyRetrospectivesReturnsMyRetrospectivesForActiveMember() {
		Retrospective existing = existingRetrospective(RetrospectiveStatus.COMPLETED);
		CapturingRepository repository = new CapturingRepository();
		repository.membership = activeMember();
		repository.myRetrospectives = List.of(existing);
		RetrospectiveService service = service(repository);

		List<Retrospective> result = service.listMyRetrospectives(new ListMyRetrospectivesQuery(USER_ID, GROUP_ID));

		assertThat(result).containsExactly(existing);
	}

	@Test
	void listMyRetrospectivesRejectsNonMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = null;
		RetrospectiveService service = service(repository);

		assertThatThrownBy(() -> service.listMyRetrospectives(new ListMyRetrospectivesQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(RetrospectiveAccessDeniedException.class);
	}

	@Test
	void listMyRetrospectivesRejectsLeftMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = new RetrospectiveMembershipContext(
			GROUP_ID, MEMBER_ID, StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.LEFT
		);
		RetrospectiveService service = service(repository);

		assertThatThrownBy(() -> service.listMyRetrospectives(new ListMyRetrospectivesQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(RetrospectiveAccessDeniedException.class);
	}

	@Test
	void listMyRetrospectivesRejectsPendingMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = new RetrospectiveMembershipContext(
			GROUP_ID, MEMBER_ID, StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.PENDING_ONBOARDING
		);
		RetrospectiveService service = service(repository);

		assertThatThrownBy(() -> service.listMyRetrospectives(new ListMyRetrospectivesQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(RetrospectiveAccessDeniedException.class);
	}

	@Test
	void requestRejectsPendingMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.membership = new RetrospectiveMembershipContext(
			GROUP_ID,
			MEMBER_ID,
			StudyGroupStatus.ACTIVE,
			GroupMemberPermission.MEMBER,
			GroupMemberStatus.PENDING_ONBOARDING
		);
		repository.progress = completedProgress();
		RetrospectiveService service = service(repository, RETROSPECTIVE_ID);

		assertThatThrownBy(() -> service.requestMyRetrospective(new RequestRetrospectiveCommand(
				USER_ID,
				WEEK_ID,
				RetrospectiveTriggerType.MANUAL
			)))
			.isInstanceOf(RetrospectiveAccessDeniedException.class)
			.hasMessage("active group membership is required to request retrospective.");
		assertThat(repository.insertedRetrospective).isNull();
	}

	@Test
	void requestRejectsLeftMemberBeforeCreatingRetrospective() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.membership = new RetrospectiveMembershipContext(
			GROUP_ID,
			MEMBER_ID,
			StudyGroupStatus.ACTIVE,
			GroupMemberPermission.MEMBER,
			GroupMemberStatus.LEFT
		);
		repository.progress = completedProgress();
		RetrospectiveService service = service(repository, RETROSPECTIVE_ID);

		assertThatThrownBy(() -> service.requestMyRetrospective(new RequestRetrospectiveCommand(
				USER_ID,
				WEEK_ID,
				RetrospectiveTriggerType.MANUAL
			)))
			.isInstanceOf(RetrospectiveAccessDeniedException.class)
			.hasMessage("active group membership is required to request retrospective.");
		assertThat(repository.insertedRetrospective).isNull();
	}

	@Test
	void getRejectsLeftMemberBeforeReturningRetrospective() {
		Retrospective existing = existingRetrospective(RetrospectiveStatus.COMPLETED);
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.membership = new RetrospectiveMembershipContext(
			GROUP_ID,
			MEMBER_ID,
			StudyGroupStatus.ACTIVE,
			GroupMemberPermission.MEMBER,
			GroupMemberStatus.LEFT
		);
		repository.progress = completedProgress();
		repository.existingRetrospective = existing;
		RetrospectiveService service = service(repository);

		assertThatThrownBy(() -> service.getMyRetrospective(new GetMyRetrospectiveQuery(USER_ID, WEEK_ID)))
			.isInstanceOf(RetrospectiveAccessDeniedException.class)
			.hasMessage("active group membership is required to read retrospective.");
	}

	@Test
	void getRejectsNonMemberWithoutReturningOtherMemberRetrospective() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.membership = null;
		repository.progress = completedProgress();
		repository.existingRetrospective = existingRetrospective(RetrospectiveStatus.COMPLETED);
		RetrospectiveService service = service(repository);

		assertThatThrownBy(() -> service.getMyRetrospective(new GetMyRetrospectiveQuery(USER_ID, WEEK_ID)))
			.isInstanceOf(RetrospectiveAccessDeniedException.class)
			.hasMessage("authenticated user is not a member of this study group.");
	}

	@Test
	void requestReturnsNotFoundWhenMemberProgressIsMissing() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.membership = activeMember();
		RetrospectiveService service = service(repository, RETROSPECTIVE_ID);

		assertThatThrownBy(() -> service.requestMyRetrospective(new RequestRetrospectiveCommand(
				USER_ID,
				WEEK_ID,
				RetrospectiveTriggerType.MANUAL
			)))
			.isInstanceOf(RetrospectiveNotFoundException.class)
			.hasMessage("member week progress was not found.");
	}

	@Test
	void getReturnsNotFoundWhenRetrospectiveIsMissing() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.membership = activeMember();
		repository.progress = completedProgress();
		RetrospectiveService service = service(repository);

		assertThatThrownBy(() -> service.getMyRetrospective(new GetMyRetrospectiveQuery(USER_ID, WEEK_ID)))
			.isInstanceOf(RetrospectiveNotFoundException.class)
			.hasMessage("retrospective was not found.");
	}

	@Test
	void applyFeedbackMapsAiResultAndCompletesRetrospective() {
		Retrospective existing = existingRetrospective(RetrospectiveStatus.PROCESSING);
		CapturingRepository repository = new CapturingRepository();
		repository.retrospectiveById = existing;
		RetrospectiveService service = service(repository);
		RetrospectiveFeedbackResult result = RetrospectiveFeedbackResult.of(
			"미완료 사유가 실습 시간 부족에 집중되어 있습니다.",
			List.of("읽기 과제 정리가 명확합니다."),
			List.of("실습 과제가 밀렸습니다."),
			List.of("다음 주 실습량을 낮춥니다."),
			Map.of("difficulty", "slightly_lower", "taskChanges", List.of("필수 실습 1개 분리"))
		);

		Retrospective completed = service.applyFeedback(new ApplyRetrospectiveFeedbackCommand(
			RETROSPECTIVE_ID,
			LLM_USAGE_ID,
			result
		));

		assertThat(completed.status()).isEqualTo(RetrospectiveStatus.COMPLETED);
		assertThat(completed.llmUsageId()).isEqualTo(LLM_USAGE_ID);
		assertThat(completed.aiFeedback()).containsEntry("summary", "미완료 사유가 실습 시간 부족에 집중되어 있습니다.");
		assertThat(completed.nextWeekAdjustment()).containsEntry("difficulty", "slightly_lower");
		assertThat(repository.updatedRetrospective).isSameAs(completed);
	}

	@Test
	void failFeedbackRecordsFailedStatusAndSafeError() {
		Retrospective existing = existingRetrospective(RetrospectiveStatus.PROCESSING);
		CapturingRepository repository = new CapturingRepository();
		repository.retrospectiveById = existing;
		RetrospectiveService service = service(repository);

		Retrospective failed = service.failFeedback(new FailRetrospectiveFeedbackCommand(
			RETROSPECTIVE_ID,
			LLM_USAGE_ID,
			"PROVIDER_TIMEOUT",
			"LLM provider timed out"
		));

		assertThat(failed.status()).isEqualTo(RetrospectiveStatus.FAILED);
		assertThat(failed.completedAt()).isNull();
		assertThat(failed.aiFeedback()).containsKey("error");
		assertThat(repository.updatedRetrospective).isSameAs(failed);
	}

	@Test
	void applyFeedbackReturnsNotFoundWhenRetrospectiveDoesNotExist() {
		CapturingRepository repository = new CapturingRepository();
		RetrospectiveService service = service(repository);

		assertThatThrownBy(() -> service.applyFeedback(new ApplyRetrospectiveFeedbackCommand(
				RETROSPECTIVE_ID,
				LLM_USAGE_ID,
				RetrospectiveFeedbackResult.of("요약", List.of(), List.of(), List.of(), Map.of())
			)))
			.isInstanceOf(RetrospectiveNotFoundException.class)
			.hasMessage("retrospective was not found.");
	}

	@Test
	void feedbackCommandsRequireLlmUsageId() {
		RetrospectiveFeedbackResult result = RetrospectiveFeedbackResult.of("요약", List.of(), List.of(), List.of(), Map.of());

		assertThatThrownBy(() -> new ApplyRetrospectiveFeedbackCommand(RETROSPECTIVE_ID, null, result))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("llmUsageId must not be null");
		assertThatThrownBy(() -> new FailRetrospectiveFeedbackCommand(
				RETROSPECTIVE_ID,
				null,
				"PROVIDER_TIMEOUT",
				"LLM provider timed out"
			))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("llmUsageId must not be null");
	}

	private static RetrospectiveService service(CapturingRepository repository, UUID... ids) {
		Queue<UUID> idQueue = new ArrayDeque<>(List.of(ids));
		return new RetrospectiveService(
			repository,
			CLOCK,
			() -> {
				UUID id = idQueue.poll();
				if (id == null) {
					throw new AssertionError("no deterministic id left");
				}
				return id;
			}
		);
	}

	private static RetrospectiveService service(
		CapturingRepository repository,
		RetrospectiveFeedbackGenerator generator,
		CapturingUsageRecorder usageRecorder,
		UUID... ids
	) {
		return service(repository, generator, usageRecorder, NotificationEventPublisher.noop(), ids);
	}

	private static RetrospectiveService service(
		CapturingRepository repository,
		RetrospectiveFeedbackGenerator generator,
		CapturingUsageRecorder usageRecorder,
		NotificationEventPublisher notificationEvents,
		UUID... ids
	) {
		Queue<UUID> idQueue = new ArrayDeque<>(List.of(ids));
		return new RetrospectiveService(
			repository,
			CLOCK,
			() -> {
				UUID id = idQueue.poll();
				if (id == null) {
					throw new AssertionError("no deterministic id left");
				}
				return id;
			},
			generator,
			usageRecorder,
			notificationEvents
		);
	}

	private static CapturingRepository readyRepository() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.membership = activeMember();
		repository.progress = completedProgress();
		repository.taskSummaries = List.of(
			taskSummary(READING_TASK_ID, WeeklyTaskType.READING, TaskCompletionStatus.DONE, "정리 완료", null),
			taskSummary(PRACTICE_TASK_ID, WeeklyTaskType.PRACTICE, TaskCompletionStatus.INCOMPLETE, null, "실습 시간 부족")
		);
		return repository;
	}

	private static RetrospectiveMembershipContext activeMember() {
		return new RetrospectiveMembershipContext(
			GROUP_ID,
			MEMBER_ID,
			StudyGroupStatus.ACTIVE,
			GroupMemberPermission.MEMBER,
			GroupMemberStatus.ACTIVE
		);
	}

	private static RetrospectiveProgress completedProgress() {
		return new RetrospectiveProgress(
			PROGRESS_ID,
			WEEK_ID,
			MEMBER_ID,
			MemberWeekProgressStatus.COMPLETED,
			NOW.minusSeconds(3600),
			WEEK_DUE_AT,
			NOW.minusSeconds(600),
			"주차 과제를 완료했습니다.",
			null,
			null
		);
	}

	private static RetrospectiveTaskSummary taskSummary(
		UUID taskId,
		WeeklyTaskType taskType,
		TaskCompletionStatus status,
		String completionNote,
		String incompleteReason
	) {
		return new RetrospectiveTaskSummary(
			taskId,
			1,
			taskType,
			taskType + " 과제",
			true,
			WEEK_DUE_AT,
			status,
			status == TaskCompletionStatus.DONE ? NOW.minusSeconds(900) : null,
			completionNote,
			incompleteReason,
			status == TaskCompletionStatus.INCOMPLETE ? NOW.minusSeconds(300) : null
		);
	}

	private static Retrospective existingRetrospective(RetrospectiveStatus status) {
		return new Retrospective(
			RETROSPECTIVE_ID,
			PROGRESS_ID,
			WEEK_ID,
			MEMBER_ID,
			null,
			RetrospectiveTriggerType.MANUAL,
			Map.of("progress", Map.of("status", "COMPLETED")),
			status == RetrospectiveStatus.COMPLETED ? Map.of("summary", "좋은 흐름입니다.") : Map.of(),
			status == RetrospectiveStatus.COMPLETED ? Map.of("focus", "JPA 심화") : Map.of(),
			status,
			NOW.minusSeconds(120),
			status == RetrospectiveStatus.COMPLETED ? NOW : null,
			NOW.minusSeconds(120),
			NOW
		);
	}

	private static RetrospectiveAiContext aiContext() {
		return new RetrospectiveAiContext(
			Map.of(
				"keywordSkillLevels", Map.of("JPA", 2),
				"taskPreferences", Map.of("PRACTICE", 5),
				"additionalNote", "실습 과제가 더 필요합니다."
			),
			List.of(Map.of(
				"id", "rule-1",
				"ruleType", "TASK_DEADLINE",
				"description", "마감 전 미완료 사유 제출"
			)),
			List.of(Map.of(
				"id", "violation-1",
				"ruleType", "TASK_DEADLINE",
				"status", "OPEN"
			)),
			List.of(Map.of(
				"id", "prior-1",
				"status", "COMPLETED",
				"aiFeedback", Map.of("summary", "지난주 실습량이 많았습니다.")
			)),
			Map.of(
				"status", "AVAILABLE",
				"summary", "사용자는 실습 시간을 줄여달라고 요청했습니다."
			)
		);
	}

	private static LlmStructuredResponse successfulResponse() {
		return new LlmStructuredResponse(
			LlmProvider.OPENAI,
			"gpt-4o-mini",
			"""
				{"summary":"미완료 사유가 실습 시간 부족에 집중되어 있습니다.","strengths":["읽기 과제 정리가 명확합니다."],"risks":["실습 과제가 밀렸습니다."],"actionItems":["다음 주 실습량을 낮춥니다."],"nextWeekAdjustment":{"difficulty":"slightly_lower","taskChanges":["필수 실습 1개 분리"]}}
				""",
			101,
			55,
			BigDecimal.ZERO,
			130,
			LlmUsageStatus.SUCCESS,
			null,
			Map.of(
				"purpose", "RETROSPECTIVE_FEEDBACK",
				"retrospectiveId", RETROSPECTIVE_ID.toString(),
				"weekId", WEEK_ID.toString(),
				"memberId", MEMBER_ID.toString(),
				"taskCount", 2,
				"ruleViolationCount", 1
			),
			"raw provider response"
		);
	}

	private static final class CapturingRepository implements RetrospectiveRepository {

		private boolean weekExists;
		private RetrospectiveMembershipContext membership;
		private RetrospectiveProgress progress;
		private Retrospective existingRetrospective;
		private Retrospective retrospectiveById;
		private List<RetrospectiveTaskSummary> taskSummaries = List.of();
		private RetrospectiveAiContext aiContext = RetrospectiveAiContext.empty();
		private Retrospective insertedRetrospective;
		private Retrospective updatedRetrospective;
		private final List<Retrospective> updatedRetrospectives = new ArrayList<>();

		@Override
		public boolean existsCurriculumWeek(UUID weekId) {
			return weekExists;
		}

		@Override
		public Optional<RetrospectiveMembershipContext> findMembershipByWeekId(UUID weekId, UUID userId) {
			return Optional.ofNullable(membership);
		}

		private List<Retrospective> myRetrospectives = List.of();

		@Override
		public Optional<RetrospectiveMembershipContext> findMembershipByGroupId(UUID groupId, UUID userId) {
			return Optional.ofNullable(membership);
		}

		@Override
		public List<Retrospective> findMyRetrospectivesByGroup(UUID groupId, UUID memberId) {
			return myRetrospectives;
		}

		@Override
		public Optional<RetrospectiveProgress> findProgress(UUID weekId, UUID memberId) {
			return Optional.ofNullable(progress);
		}

		@Override
		public Optional<Retrospective> findRetrospective(UUID progressId, UUID weekId, UUID memberId) {
			return Optional.ofNullable(existingRetrospective);
		}

		@Override
		public List<RetrospectiveTaskSummary> findTaskSummaries(UUID progressId, UUID weekId, UUID memberId) {
			return taskSummaries;
		}

		@Override
		public RetrospectiveAiContext findAiContext(UUID groupId, UUID memberId, UUID weekId, UUID retrospectiveId) {
			return aiContext;
		}

		@Override
		public boolean insertRetrospective(Retrospective retrospective) {
			insertedRetrospective = retrospective;
			return true;
		}

		@Override
		public Optional<Retrospective> findRetrospectiveById(UUID retrospectiveId) {
			return Optional.ofNullable(retrospectiveById);
		}

		@Override
		public boolean updateRetrospectiveResult(Retrospective retrospective) {
			updatedRetrospective = retrospective;
			updatedRetrospectives.add(retrospective);
			return true;
		}

		@Override
		public boolean updateRetrospectiveAnswers(Retrospective retrospective) {
			updatedRetrospective = retrospective;
			updatedRetrospectives.add(retrospective);
			return true;
		}

		java.util.List<com.studypot.aistudyleader.retrospective.domain.RetrospectiveWeekOverview> overview = java.util.List.of();

		@Override
		public java.util.List<com.studypot.aistudyleader.retrospective.domain.RetrospectiveWeekOverview> findRetrospectiveOverview(UUID groupId, UUID memberId) {
			return overview;
		}

		private boolean writable = true;

		@Override
		public boolean isRetrospectiveWritable(UUID weekId, UUID memberId) {
			return writable;
		}

		private List<RetrospectiveStatus> updatedStatuses() {
			return updatedRetrospectives.stream()
				.map(Retrospective::status)
				.toList();
		}
	}

	private static final class CapturingFeedbackGenerator implements RetrospectiveFeedbackGenerator {

		private final RetrospectiveFeedbackGeneration generation;
		private Map<String, Object> inputSummary;

		private CapturingFeedbackGenerator(RetrospectiveFeedbackGeneration generation) {
			this.generation = generation;
		}

		@Override
		public RetrospectiveFeedbackGeneration generate(Retrospective retrospective) {
			inputSummary = retrospective.inputSummary();
			return generation;
		}
	}

	private static final class FailingFeedbackGenerator implements RetrospectiveFeedbackGenerator {

		private final LlmCallFailure failure;

		private FailingFeedbackGenerator(LlmCallFailure failure) {
			this.failure = failure;
		}

		@Override
		public RetrospectiveFeedbackGeneration generate(Retrospective retrospective) {
			throw new RetrospectiveFeedbackGenerationException("retrospective feedback generation failed.", failure);
		}
	}

	private static final class CapturingUsageRecorder implements LlmUsageRecorder {

		private LlmUsage usage;

		@Override
		public void record(LlmUsage usage) {
			this.usage = usage;
		}
	}

	private record RetrospectiveNotice(UUID groupId, UUID recipientUserId, UUID retrospectiveId, UUID weekId) {
	}

	private static final class CapturingNotificationPublisher implements NotificationEventPublisher {

		private final List<RetrospectiveNotice> retrospectiveReady = new ArrayList<>();
		private final List<RetrospectiveNotice> nextWeekAdjusted = new ArrayList<>();

		@Override
		public void publishGroupDeleted(UUID groupId, UUID recipientUserId, String groupName) {
		}

		@Override
		public void publishNoticePosted(UUID groupId, UUID actorUserId, UUID postId, String title) {
		}

		@Override
		public void publishLeaderReportPosted(UUID groupId, UUID postId, String title) {
		}

		@Override
		public void publishStudyCompleted(UUID groupId, String groupName) {
		}

		@Override
		public void publishOnboardingCompleted(UUID groupId, UUID ownerUserId) {
		}

		@Override
		public void publishMemberJoined(UUID groupId, UUID ownerUserId, UUID joinedUserId) {
		}

		@Override
		public void publishOnboardingSubmitted(UUID groupId, UUID recipientUserId, UUID submitterMemberId) {
		}

		@Override
		public void publishRetrospectiveReminder(UUID groupId, UUID recipientUserId, UUID weekId) {
		}

		@Override
		public void publishRetrospectiveFinalReminder(UUID groupId, UUID recipientUserId, UUID weekId) {
		}

		@Override
		public void publishOnboardingRequested(UUID groupId, UUID recipientUserId) {
		}

		@Override
		public void publishWeekStarted(UUID groupId, UUID weekId, int weekNumber, String weekTitle) {
		}

		@Override
		public void publishTaskDueReminder(
			UUID groupId,
			UUID recipientUserId,
			UUID weekId,
			UUID taskCompletionId,
			String taskTitle,
			Instant dueAt
		) {
		}

		@Override
		public void publishTaskOverdueCheck(UUID groupId, UUID recipientUserId, UUID taskCompletionId, String taskTitle) {
		}

		@Override
		public void publishIncompleteReasonRequested(UUID groupId, UUID recipientUserId, UUID taskCompletionId, String taskTitle) {
		}

		@Override
		public void publishRetrospectiveReady(UUID groupId, UUID recipientUserId, UUID retrospectiveId, UUID weekId) {
			retrospectiveReady.add(new RetrospectiveNotice(groupId, recipientUserId, retrospectiveId, weekId));
		}

		@Override
		public void publishNextWeekAdjusted(UUID groupId, UUID recipientUserId, UUID retrospectiveId, UUID weekId) {
			nextWeekAdjusted.add(new RetrospectiveNotice(groupId, recipientUserId, retrospectiveId, weekId));
		}
	}
}
