package com.studypot.aistudyleader.curriculum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.curriculum.domain.Curriculum;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGeneration;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGenerationRequest;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStartContext;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStatus;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeek;
import com.studypot.aistudyleader.curriculum.domain.CurriculumTaskPlan;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeekStatus;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeekPlan;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgress;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgressStatus;
import com.studypot.aistudyleader.curriculum.domain.SubmittedAvailabilitySlot;
import com.studypot.aistudyleader.curriculum.domain.SubmittedOnboardingResponse;
import com.studypot.aistudyleader.curriculum.domain.TaskCompletion;
import com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTask;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import com.studypot.aistudyleader.curriculum.repository.CurriculumRepository;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CurriculumServiceTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000004021");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000004022");
	private static final UUID OWNER_MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000004023");
	private static final UUID RESPONSE_ID = UUID.fromString("018f0000-0000-7000-8000-000000004024");
	private static final UUID LLM_USAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000004025");
	private static final UUID CURRICULUM_ID = UUID.fromString("018f0000-0000-7000-8000-000000004026");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000004027");
	private static final UUID TASK_ID = UUID.fromString("018f0000-0000-7000-8000-000000004028");
	private static final UUID SECOND_TASK_ID = UUID.fromString("018f0000-0000-7000-8000-000000004029");
	private static final UUID PROGRESS_ID = UUID.fromString("018f0000-0000-7000-8000-000000004030");
	private static final UUID COMPLETION_ID = UUID.fromString("018f0000-0000-7000-8000-000000004031");
	private static final Instant NOW = Instant.parse("2026-05-11T01:15:00Z");
	private static final Instant WEEK_DUE_AT = Instant.parse("2026-05-18T01:15:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Test
	void startStudyCreatesCurriculumFromSubmittedOnboardingAndActivatesGroup() {
		CapturingRepository repository = new CapturingRepository();
		repository.startContext = ownerStartContext(StudyGroupStatus.ONBOARDING, GroupMemberStatus.ACTIVE);
		repository.submittedResponses = List.of(submittedResponse());
		CurriculumService service = service(repository, generation(), LLM_USAGE_ID, CURRICULUM_ID, WEEK_ID, TASK_ID);

		Curriculum result = service.startStudy(new StartCurriculumCommand(USER_ID, GROUP_ID));

		assertThat(repository.savedGroupId).isEqualTo(GROUP_ID);
		assertThat(repository.savedStartedAt).isEqualTo(NOW);
		assertThat(repository.savedLlmUsage.id()).isEqualTo(LLM_USAGE_ID);
		assertThat(repository.savedLlmUsage.purpose()).isEqualTo(LlmUsagePurpose.CURRICULUM_GENERATE);
		assertThat(repository.savedLlmUsage.provider()).isEqualTo(LlmProvider.OPENAI);
		assertThat(repository.savedCurriculum).isSameAs(result);
		assertThat(result.id()).isEqualTo(CURRICULUM_ID);
		assertThat(result.groupId()).isEqualTo(GROUP_ID);
		assertThat(result.llmUsageId()).contains(LLM_USAGE_ID);
		assertThat(result.status()).isEqualTo(CurriculumStatus.ACTIVE);
		assertThat(result.onboardingSummary())
			.containsEntry("submittedResponseCount", 1)
			.containsEntry("generatedAt", NOW.toString());
		assertThat(result.weeks()).hasSize(1);
		assertThat(result.weeks().getFirst().id()).isEqualTo(WEEK_ID);
		assertThat(result.weeks().getFirst().tasks()).hasSize(1);
		assertThat(result.weeks().getFirst().tasks().getFirst().id()).isEqualTo(TASK_ID);
		assertThat(repository.generationRequest.onboardingSummary())
			.containsEntry("submittedResponseCount", 1);
	}

	@Test
	void startStudyRejectsWhenGeneratorIsNotConfigured() {
		CapturingRepository repository = new CapturingRepository();
		repository.startContext = ownerStartContext(StudyGroupStatus.ONBOARDING, GroupMemberStatus.ACTIVE);
		repository.submittedResponses = List.of(submittedResponse());
		CurriculumService service = new CurriculumService(repository, () -> null, CLOCK, () -> CURRICULUM_ID);

		assertThatThrownBy(() -> service.startStudy(new StartCurriculumCommand(USER_ID, GROUP_ID)))
			.isInstanceOf(CurriculumGenerationException.class)
			.hasMessage("curriculum generator is not configured.");
		assertThat(repository.savedCurriculum).isNull();
	}

	@Test
	void startStudyRejectsNonOwner() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.startContext = memberStartContext(StudyGroupStatus.ONBOARDING, GroupMemberStatus.ACTIVE);
		CurriculumService service = service(repository, generation(), LLM_USAGE_ID, CURRICULUM_ID, WEEK_ID, TASK_ID);

		assertThatThrownBy(() -> service.startStudy(new StartCurriculumCommand(USER_ID, GROUP_ID)))
			.isInstanceOf(CurriculumAccessDeniedException.class)
			.hasMessage("only the study group owner can start the curriculum.");
		assertThat(repository.savedCurriculum).isNull();
	}

	@Test
	void startStudyRejectsGroupThatIsNotOnboarding() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.startContext = ownerStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		CurriculumService service = service(repository, generation(), LLM_USAGE_ID, CURRICULUM_ID, WEEK_ID, TASK_ID);

		assertThatThrownBy(() -> service.startStudy(new StartCurriculumCommand(USER_ID, GROUP_ID)))
			.isInstanceOf(CurriculumStartRejectedException.class)
			.hasMessage("study group must be ONBOARDING to start curriculum generation.");
		assertThat(repository.savedCurriculum).isNull();
	}

	@Test
	void startStudyRejectsOwnerWhoHasNotSubmittedOnboarding() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.startContext = ownerStartContext(StudyGroupStatus.ONBOARDING, GroupMemberStatus.PENDING_ONBOARDING);
		CurriculumService service = service(repository, generation(), LLM_USAGE_ID, CURRICULUM_ID, WEEK_ID, TASK_ID);

		assertThatThrownBy(() -> service.startStudy(new StartCurriculumCommand(USER_ID, GROUP_ID)))
			.isInstanceOf(CurriculumStartRejectedException.class)
			.hasMessage("owner onboarding must be submitted before starting the study.");
		assertThat(repository.savedCurriculum).isNull();
	}

	@Test
	void getCurriculumReturnsActiveCurriculumForActiveMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.readContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		repository.activeCurriculum = generation().toCurriculum(
			CURRICULUM_ID,
			GROUP_ID,
			LLM_USAGE_ID,
			Map.of("submittedResponseCount", 1),
			NOW,
			List.of(WEEK_ID),
			List.of(TASK_ID)
		);
		CurriculumService service = service(repository, generation(), LLM_USAGE_ID, CURRICULUM_ID, WEEK_ID, TASK_ID);

		Curriculum result = service.getCurriculum(new GetCurriculumQuery(USER_ID, GROUP_ID));

		assertThat(result).isSameAs(repository.activeCurriculum);
	}

	@Test
	void getCurriculumRejectsPendingMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.readContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.PENDING_ONBOARDING);
		CurriculumService service = service(repository, generation(), LLM_USAGE_ID, CURRICULUM_ID, WEEK_ID, TASK_ID);

		assertThatThrownBy(() -> service.getCurriculum(new GetCurriculumQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(CurriculumAccessDeniedException.class)
			.hasMessage("active group membership is required to read the curriculum.");
	}

	@Test
	void getCurrentWeekReturnsInProgressWeekForActiveMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.readContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		repository.currentWeek = currentWeek();
		CurriculumService service = service(repository, generation(), LLM_USAGE_ID, CURRICULUM_ID, WEEK_ID, TASK_ID);

		CurriculumWeek result = service.getCurrentWeek(new GetCurrentWeekQuery(USER_ID, GROUP_ID));

		assertThat(result).isSameAs(repository.currentWeek);
	}

	@Test
	void getCurrentWeekRejectsPendingMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.readContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.PENDING_ONBOARDING);
		CurriculumService service = service(repository, generation(), LLM_USAGE_ID, CURRICULUM_ID, WEEK_ID, TASK_ID);

		assertThatThrownBy(() -> service.getCurrentWeek(new GetCurrentWeekQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(CurriculumAccessDeniedException.class)
			.hasMessage("active group membership is required to read the current week.");
	}

	@Test
	void getCurrentWeekRejectsNonMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		CurriculumService service = service(repository, generation(), LLM_USAGE_ID, CURRICULUM_ID, WEEK_ID, TASK_ID);

		assertThatThrownBy(() -> service.getCurrentWeek(new GetCurrentWeekQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(CurriculumAccessDeniedException.class)
			.hasMessage("authenticated user is not a member of this study group.");
	}

	@Test
	void getCurrentWeekRejectsWhenNoCurrentWeekExists() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		repository.readContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		CurriculumService service = service(repository, generation(), LLM_USAGE_ID, CURRICULUM_ID, WEEK_ID, TASK_ID);

		assertThatThrownBy(() -> service.getCurrentWeek(new GetCurrentWeekQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(CurriculumNotFoundException.class)
			.hasMessage("current curriculum week was not found.");
	}

	@Test
	void listWeeklyTasksReturnsTasksForActiveWeekMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.weekReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		repository.weeklyTasks = List.of(task(TASK_ID, 1, WeeklyTaskType.READING), task(SECOND_TASK_ID, 2, WeeklyTaskType.PROJECT));
		CurriculumService service = service(repository, generation(), LLM_USAGE_ID, CURRICULUM_ID, WEEK_ID, TASK_ID);

		List<WeeklyTask> result = service.listWeeklyTasks(new ListWeeklyTasksQuery(USER_ID, WEEK_ID));

		assertThat(result).containsExactlyElementsOf(repository.weeklyTasks);
	}

	@Test
	void listWeeklyTasksRejectsPendingWeekMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.weekReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.PENDING_ONBOARDING);
		CurriculumService service = service(repository, generation(), LLM_USAGE_ID, CURRICULUM_ID, WEEK_ID, TASK_ID);

		assertThatThrownBy(() -> service.listWeeklyTasks(new ListWeeklyTasksQuery(USER_ID, WEEK_ID)))
			.isInstanceOf(CurriculumAccessDeniedException.class)
			.hasMessage("active group membership is required to read weekly tasks.");
	}

	@Test
	void listWeeklyTasksRejectsNonMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		CurriculumService service = service(repository, generation(), LLM_USAGE_ID, CURRICULUM_ID, WEEK_ID, TASK_ID);

		assertThatThrownBy(() -> service.listWeeklyTasks(new ListWeeklyTasksQuery(USER_ID, WEEK_ID)))
			.isInstanceOf(CurriculumAccessDeniedException.class)
			.hasMessage("authenticated user is not a member of this study group.");
	}

	@Test
	void listWeeklyTasksReturnsNotFoundWhenWeekDoesNotExist() {
		CapturingRepository repository = new CapturingRepository();
		CurriculumService service = service(repository, generation(), LLM_USAGE_ID, CURRICULUM_ID, WEEK_ID, TASK_ID);

		assertThatThrownBy(() -> service.listWeeklyTasks(new ListWeeklyTasksQuery(USER_ID, WEEK_ID)))
			.isInstanceOf(CurriculumNotFoundException.class)
			.hasMessage("curriculum week was not found.");
	}

	@Test
	void getMyWeekProgressReturnsExistingProgressForActiveMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.weekReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		repository.existingProgress = progress(MemberWeekProgressStatus.IN_PROGRESS, NOW.minusSeconds(60), null, null, null, null);
		CurriculumService service = service(repository, generation(), PROGRESS_ID);

		MemberWeekProgress result = service.getMyWeekProgress(new GetWeekProgressQuery(USER_ID, WEEK_ID));

		assertThat(result).isSameAs(repository.existingProgress);
		assertThat(repository.insertedProgress).isNull();
		assertThat(repository.updatedProgress).isNull();
	}

	@Test
	void getMyWeekProgressReturnsNotFoundWithoutCreatingWhenProgressIsMissing() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.weekReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		CurriculumService service = service(repository, generation(), PROGRESS_ID);

		assertThatThrownBy(() -> service.getMyWeekProgress(new GetWeekProgressQuery(USER_ID, WEEK_ID)))
			.isInstanceOf(CurriculumNotFoundException.class)
			.hasMessage("member week progress was not found.");
		assertThat(repository.insertedProgress).isNull();
		assertThat(repository.updatedProgress).isNull();
	}

	@Test
	void getMyWeekProgressRejectsPendingMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.weekReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.PENDING_ONBOARDING);
		repository.existingProgress = progress(MemberWeekProgressStatus.IN_PROGRESS, NOW.minusSeconds(60), null, null, null, null);
		CurriculumService service = service(repository, generation(), PROGRESS_ID);

		assertThatThrownBy(() -> service.getMyWeekProgress(new GetWeekProgressQuery(USER_ID, WEEK_ID)))
			.isInstanceOf(CurriculumAccessDeniedException.class)
			.hasMessage("active group membership is required to read week progress.");
	}

	@Test
	void getMyWeekProgressReturnsNotFoundWhenWeekDoesNotExist() {
		CapturingRepository repository = new CapturingRepository();
		CurriculumService service = service(repository, generation(), PROGRESS_ID);

		assertThatThrownBy(() -> service.getMyWeekProgress(new GetWeekProgressQuery(USER_ID, WEEK_ID)))
			.isInstanceOf(CurriculumNotFoundException.class)
			.hasMessage("curriculum week was not found.");
	}

	@Test
	void updateMyWeekProgressCreatesProgressForActiveMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.weekReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		repository.weekDueAt = WEEK_DUE_AT;
		CurriculumService service = service(repository, generation(), PROGRESS_ID);

		MemberWeekProgress result = service.updateMyWeekProgress(new UpdateWeekProgressCommand(
			USER_ID,
			WEEK_ID,
			MemberWeekProgressStatus.NOT_STARTED,
			null,
			null
		));

		assertThat(result.id()).isEqualTo(PROGRESS_ID);
		assertThat(result.curriculumWeekId()).isEqualTo(WEEK_ID);
		assertThat(result.memberId()).isEqualTo(OWNER_MEMBER_ID);
		assertThat(result.status()).isEqualTo(MemberWeekProgressStatus.NOT_STARTED);
		assertThat(result.dueAt()).isEqualTo(WEEK_DUE_AT);
		assertThat(repository.insertedProgress).isSameAs(result);
		assertThat(repository.updatedProgress).isNull();
	}

	@Test
	void updateMyWeekProgressUpdatesExistingProgressToInProgress() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.weekReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		repository.existingProgress = progress(MemberWeekProgressStatus.NOT_STARTED, null, null, null, null, null);
		CurriculumService service = service(repository, generation(), PROGRESS_ID);

		MemberWeekProgress result = service.updateMyWeekProgress(new UpdateWeekProgressCommand(
			USER_ID,
			WEEK_ID,
			MemberWeekProgressStatus.IN_PROGRESS,
			null,
			null
		));

		assertThat(result.id()).isEqualTo(PROGRESS_ID);
		assertThat(result.status()).isEqualTo(MemberWeekProgressStatus.IN_PROGRESS);
		assertThat(result.startedAt()).isEqualTo(NOW);
		assertThat(repository.insertedProgress).isNull();
		assertThat(repository.updatedProgress).isSameAs(result);
	}

	@Test
	void updateMyWeekProgressAppliesRequestWhenInsertRacesWithExistingProgress() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.weekReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		repository.weekDueAt = WEEK_DUE_AT;
		repository.insertSucceeds = false;
		repository.raceProgress = progress(MemberWeekProgressStatus.NOT_STARTED, null, null, null, null, null);
		CurriculumService service = service(repository, generation(), PROGRESS_ID);

		MemberWeekProgress result = service.updateMyWeekProgress(new UpdateWeekProgressCommand(
			USER_ID,
			WEEK_ID,
			MemberWeekProgressStatus.IN_PROGRESS,
			null,
			null
		));

		assertThat(result.status()).isEqualTo(MemberWeekProgressStatus.IN_PROGRESS);
		assertThat(result.startedAt()).isEqualTo(NOW);
		assertThat(repository.updatedProgress).isSameAs(result);
	}

	@Test
	void updateMyWeekProgressPreservesFirstCompletionTimestampAndNote() {
		Instant completedAt = NOW.minusSeconds(120);
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.weekReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		repository.existingProgress = progress(
			MemberWeekProgressStatus.COMPLETED,
			NOW.minusSeconds(300),
			completedAt,
			"처음 제출",
			null,
			null
		);
		CurriculumService service = service(repository, generation(), PROGRESS_ID);

		MemberWeekProgress result = service.updateMyWeekProgress(new UpdateWeekProgressCommand(
			USER_ID,
			WEEK_ID,
			MemberWeekProgressStatus.COMPLETED,
			"수정 제출",
			null
		));

		assertThat(result.completedAt()).isEqualTo(completedAt);
		assertThat(result.completionNote()).isEqualTo("처음 제출");
	}

	@Test
	void updateMyWeekProgressRejectsGoingBackToInProgressAfterCompleted() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.weekReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		repository.existingProgress = progress(
			MemberWeekProgressStatus.COMPLETED,
			NOW.minusSeconds(300),
			NOW.minusSeconds(120),
			"완료",
			null,
			null
		);
		CurriculumService service = service(repository, generation(), PROGRESS_ID);

		assertThatThrownBy(() -> service.updateMyWeekProgress(new UpdateWeekProgressCommand(
				USER_ID,
				WEEK_ID,
				MemberWeekProgressStatus.IN_PROGRESS,
				null,
				null
			)))
			.isInstanceOf(InvalidWeekProgressRequestException.class)
			.hasMessage("member week progress cannot transition from COMPLETED to IN_PROGRESS.");
	}

	@Test
	void updateMyWeekProgressRejectsPendingMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.weekReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.PENDING_ONBOARDING);
		CurriculumService service = service(repository, generation(), PROGRESS_ID);

		assertThatThrownBy(() -> service.updateMyWeekProgress(new UpdateWeekProgressCommand(
				USER_ID,
				WEEK_ID,
				MemberWeekProgressStatus.IN_PROGRESS,
				null,
				null
			)))
			.isInstanceOf(CurriculumAccessDeniedException.class)
			.hasMessage("active group membership is required to update week progress.");
	}

	@Test
	void updateMyWeekProgressRejectsIncompleteWithoutReason() {
		CapturingRepository repository = new CapturingRepository();
		repository.weekExists = true;
		repository.weekReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		repository.weekDueAt = WEEK_DUE_AT;
		CurriculumService service = service(repository, generation(), PROGRESS_ID);

		assertThatThrownBy(() -> service.updateMyWeekProgress(new UpdateWeekProgressCommand(
				USER_ID,
				WEEK_ID,
				MemberWeekProgressStatus.INCOMPLETE,
				null,
				null
			)))
			.isInstanceOf(InvalidWeekProgressRequestException.class)
			.hasMessage("incomplete reason is required when status is INCOMPLETE.");
	}

	@Test
	void updateMyWeekProgressReturnsNotFoundWhenWeekDoesNotExist() {
		CapturingRepository repository = new CapturingRepository();
		CurriculumService service = service(repository, generation(), PROGRESS_ID);

		assertThatThrownBy(() -> service.updateMyWeekProgress(new UpdateWeekProgressCommand(
				USER_ID,
				WEEK_ID,
				MemberWeekProgressStatus.IN_PROGRESS,
				null,
				null
			)))
			.isInstanceOf(CurriculumNotFoundException.class)
			.hasMessage("curriculum week was not found.");
	}

	@Test
	void completeMyTaskStoresDoneCompletionForActiveMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.taskExists = true;
		repository.taskReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		repository.weeklyTask = task(TASK_ID, 1, WeeklyTaskType.READING, NOW.plusSeconds(3600));
		repository.existingProgress = progress(MemberWeekProgressStatus.IN_PROGRESS, NOW.minusSeconds(60), null, null, null, null);
		CurriculumService service = service(repository, generation(), COMPLETION_ID);

		TaskCompletion result = service.completeMyTask(new CompleteTaskCommand(
			USER_ID,
			TASK_ID,
			TaskCompletionStatus.DONE,
			"정리 완료",
			null,
			"https://example.com/evidence"
		));

		assertThat(result.id()).isEqualTo(COMPLETION_ID);
		assertThat(result.progressId()).isEqualTo(PROGRESS_ID);
		assertThat(result.weeklyTaskId()).isEqualTo(TASK_ID);
		assertThat(result.memberId()).isEqualTo(OWNER_MEMBER_ID);
		assertThat(result.status()).isEqualTo(TaskCompletionStatus.DONE);
		assertThat(result.completedAt()).isEqualTo(NOW);
		assertThat(result.completionNote()).isEqualTo("정리 완료");
		assertThat(result.evidenceUrl()).isEqualTo("https://example.com/evidence");
		assertThat(repository.insertedTaskCompletion).isSameAs(result);
		assertThat(repository.updatedTaskCompletion).isNull();
	}

	@Test
	void completeMyTaskStoresIncompleteReasonAfterDueAt() {
		CapturingRepository repository = new CapturingRepository();
		repository.taskExists = true;
		repository.taskReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		repository.weeklyTask = task(TASK_ID, 1, WeeklyTaskType.ASSIGNMENT, NOW.minusSeconds(60));
		repository.existingProgress = progress(MemberWeekProgressStatus.IN_PROGRESS, NOW.minusSeconds(3600), null, null, null, null);
		CurriculumService service = service(repository, generation(), COMPLETION_ID);

		TaskCompletion result = service.completeMyTask(new CompleteTaskCommand(
			USER_ID,
			TASK_ID,
			TaskCompletionStatus.INCOMPLETE,
			null,
			"이번 주 실습을 끝내지 못했습니다.",
			null
		));

		assertThat(result.status()).isEqualTo(TaskCompletionStatus.INCOMPLETE);
		assertThat(result.incompleteReason()).isEqualTo("이번 주 실습을 끝내지 못했습니다.");
		assertThat(result.reasonSubmittedAt()).isEqualTo(NOW);
		assertThat(repository.insertedTaskCompletion).isSameAs(result);
	}

	@Test
	void completeMyTaskStoresSkippedStatus() {
		CapturingRepository repository = new CapturingRepository();
		repository.taskExists = true;
		repository.taskReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		repository.weeklyTask = task(TASK_ID, 1, WeeklyTaskType.CUSTOM, NOW.plusSeconds(3600));
		repository.existingProgress = progress(MemberWeekProgressStatus.IN_PROGRESS, NOW.minusSeconds(60), null, null, null, null);
		CurriculumService service = service(repository, generation(), COMPLETION_ID);

		TaskCompletion result = service.completeMyTask(new CompleteTaskCommand(
			USER_ID,
			TASK_ID,
			TaskCompletionStatus.SKIPPED,
			null,
			null,
			null
		));

		assertThat(result.status()).isEqualTo(TaskCompletionStatus.SKIPPED);
		assertThat(result.completedAt()).isNull();
		assertThat(result.reasonSubmittedAt()).isNull();
	}

	@Test
	void completeMyTaskUpdatesExistingTodoInsteadOfDuplicating() {
		CapturingRepository repository = new CapturingRepository();
		repository.taskExists = true;
		repository.taskReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		repository.weeklyTask = task(TASK_ID, 1, WeeklyTaskType.READING, NOW.plusSeconds(3600));
		repository.existingProgress = progress(MemberWeekProgressStatus.IN_PROGRESS, NOW.minusSeconds(60), null, null, null, null);
		repository.existingTaskCompletion = completion(TaskCompletionStatus.TODO, null, null, null, null, null);
		CurriculumService service = service(repository, generation(), COMPLETION_ID);

		TaskCompletion result = service.completeMyTask(new CompleteTaskCommand(
			USER_ID,
			TASK_ID,
			TaskCompletionStatus.DONE,
			"완료",
			null,
			null
		));

		assertThat(result.id()).isEqualTo(COMPLETION_ID);
		assertThat(result.status()).isEqualTo(TaskCompletionStatus.DONE);
		assertThat(repository.insertedTaskCompletion).isNull();
		assertThat(repository.updatedTaskCompletion).isSameAs(result);
	}

	@Test
	void completeMyTaskRejectsChangingTerminalStatus() {
		CapturingRepository repository = new CapturingRepository();
		repository.taskExists = true;
		repository.taskReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		repository.weeklyTask = task(TASK_ID, 1, WeeklyTaskType.ASSIGNMENT, NOW.minusSeconds(60));
		repository.existingProgress = progress(MemberWeekProgressStatus.IN_PROGRESS, NOW.minusSeconds(3600), null, null, null, null);
		repository.existingTaskCompletion = completion(TaskCompletionStatus.DONE, NOW.minusSeconds(120), "완료", null, null, null);
		CurriculumService service = service(repository, generation(), COMPLETION_ID);

		assertThatThrownBy(() -> service.completeMyTask(new CompleteTaskCommand(
				USER_ID,
				TASK_ID,
				TaskCompletionStatus.INCOMPLETE,
				null,
				"늦었습니다.",
				null
			)))
			.isInstanceOf(InvalidTaskCompletionRequestException.class)
			.hasMessage("task completion cannot transition from DONE to INCOMPLETE.");
	}

	@Test
	void completeMyTaskRejectsDoneAfterDueAt() {
		CapturingRepository repository = new CapturingRepository();
		repository.taskExists = true;
		repository.taskReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		repository.weeklyTask = task(TASK_ID, 1, WeeklyTaskType.READING, NOW.minusSeconds(60));
		repository.existingProgress = progress(MemberWeekProgressStatus.IN_PROGRESS, NOW.minusSeconds(3600), null, null, null, null);
		CurriculumService service = service(repository, generation(), COMPLETION_ID);

		assertThatThrownBy(() -> service.completeMyTask(new CompleteTaskCommand(
				USER_ID,
				TASK_ID,
				TaskCompletionStatus.DONE,
				"늦은 완료",
				null,
				null
			)))
			.isInstanceOf(InvalidTaskCompletionRequestException.class)
			.hasMessage("task is overdue; submit incomplete reason.");
	}

	@Test
	void completeMyTaskRejectsIncompleteWithoutReason() {
		CapturingRepository repository = new CapturingRepository();
		repository.taskExists = true;
		repository.taskReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		repository.weeklyTask = task(TASK_ID, 1, WeeklyTaskType.READING, NOW.minusSeconds(60));
		repository.existingProgress = progress(MemberWeekProgressStatus.IN_PROGRESS, NOW.minusSeconds(3600), null, null, null, null);
		CurriculumService service = service(repository, generation(), COMPLETION_ID);

		assertThatThrownBy(() -> service.completeMyTask(new CompleteTaskCommand(
				USER_ID,
				TASK_ID,
				TaskCompletionStatus.INCOMPLETE,
				null,
				null,
				null
			)))
			.isInstanceOf(InvalidTaskCompletionRequestException.class)
			.hasMessage("incomplete reason is required when status is INCOMPLETE.");
	}

	@Test
	void completeMyTaskRejectsPendingMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.taskExists = true;
		repository.taskReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.PENDING_ONBOARDING);
		CurriculumService service = service(repository, generation(), COMPLETION_ID);

		assertThatThrownBy(() -> service.completeMyTask(new CompleteTaskCommand(
				USER_ID,
				TASK_ID,
				TaskCompletionStatus.DONE,
				null,
				null,
				null
			)))
			.isInstanceOf(CurriculumAccessDeniedException.class)
			.hasMessage("active group membership is required to complete weekly tasks.");
	}

	@Test
	void completeMyTaskRejectsWhenProgressDoesNotExist() {
		CapturingRepository repository = new CapturingRepository();
		repository.taskExists = true;
		repository.taskReadContext = memberStartContext(StudyGroupStatus.ACTIVE, GroupMemberStatus.ACTIVE);
		repository.weeklyTask = task(TASK_ID, 1, WeeklyTaskType.READING, NOW.plusSeconds(3600));
		CurriculumService service = service(repository, generation(), COMPLETION_ID);

		assertThatThrownBy(() -> service.completeMyTask(new CompleteTaskCommand(
				USER_ID,
				TASK_ID,
				TaskCompletionStatus.DONE,
				null,
				null,
				null
			)))
			.isInstanceOf(CurriculumNotFoundException.class)
			.hasMessage("member week progress was not found.");
	}

	private static CurriculumService service(CapturingRepository repository, CurriculumGeneration generation, UUID... ids) {
		Queue<UUID> idQueue = new ArrayDeque<>(List.of(ids));
		return new CurriculumService(
			repository,
			request -> {
				repository.generationRequest = request;
				return generation;
			},
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

	private static CurriculumStartContext ownerStartContext(StudyGroupStatus status, GroupMemberStatus memberStatus) {
		return new CurriculumStartContext(
			GROUP_ID,
			"Backend Interview Study",
			"Spring Boot",
			List.of("JPA", "Security"),
			status,
			LocalDate.parse("2026-05-11"),
			LocalDate.parse("2026-06-21"),
			OWNER_MEMBER_ID,
			GroupMemberPermission.OWNER,
			memberStatus
		);
	}

	private static CurriculumStartContext memberStartContext(StudyGroupStatus groupStatus, GroupMemberStatus memberStatus) {
		return new CurriculumStartContext(
			GROUP_ID,
			"Backend Interview Study",
			"Spring Boot",
			List.of("JPA", "Security"),
			groupStatus,
			LocalDate.parse("2026-05-11"),
			LocalDate.parse("2026-06-21"),
			OWNER_MEMBER_ID,
			GroupMemberPermission.MEMBER,
			memberStatus
		);
	}

	private static SubmittedOnboardingResponse submittedResponse() {
		return new SubmittedOnboardingResponse(
			RESPONSE_ID,
			OWNER_MEMBER_ID,
			Map.of("JPA", 2, "Security", 4),
			Map.of("READING", 3, "PRACTICE", 5),
			"실습 위주가 좋아요.",
			List.of(new SubmittedAvailabilitySlot(2, "20:00", "22:00", "Asia/Seoul")),
			Instant.parse("2026-05-10T08:00:00Z")
		);
	}

	private static CurriculumWeek currentWeek() {
		return new CurriculumWeek(
			WEEK_ID,
			CURRICULUM_ID,
			1,
			"JPA 기초와 환경 구성",
			"공통 환경을 만들고 핵심 개념을 맞춥니다.",
			"Entity 매핑 이해",
			List.of("Entity 매핑 이해"),
			List.of(Map.of("title", "공식 문서", "url", "https://spring.io/projects/spring-boot")),
			CurriculumWeekStatus.IN_PROGRESS,
			NOW,
			NOW.plusSeconds(604800),
			List.of(task(TASK_ID, 1, WeeklyTaskType.READING)),
			NOW,
			NOW
		);
	}

	private static WeeklyTask task(UUID id, int displayOrder, WeeklyTaskType taskType) {
		return task(id, displayOrder, taskType, NOW.plusSeconds(604800));
	}

	private static WeeklyTask task(UUID id, int displayOrder, WeeklyTaskType taskType, Instant dueAt) {
		return new WeeklyTask(
			id,
			WEEK_ID,
			displayOrder,
			taskType,
			displayOrder == 1 ? "JPA 엔티티 매핑 읽기" : "프로젝트 과제",
			displayOrder == 1 ? "핵심 매핑 규칙을 정리합니다." : "작은 예제를 완성합니다.",
			displayOrder == 1,
			dueAt,
			true,
			Map.of("displayOrder", displayOrder),
			NOW,
			NOW
		);
	}

	private static TaskCompletion completion(
		TaskCompletionStatus status,
		Instant completedAt,
		String completionNote,
		String incompleteReason,
		Instant reasonSubmittedAt,
		String evidenceUrl
	) {
		return new TaskCompletion(
			COMPLETION_ID,
			PROGRESS_ID,
			TASK_ID,
			OWNER_MEMBER_ID,
			status,
			WEEK_DUE_AT,
			completedAt,
			completionNote,
			incompleteReason,
			reasonSubmittedAt,
			evidenceUrl,
			NOW.minusSeconds(300),
			NOW.minusSeconds(300)
		);
	}

	private static MemberWeekProgress progress(
		MemberWeekProgressStatus status,
		Instant startedAt,
		Instant completedAt,
		String completionNote,
		String incompleteReason,
		Instant reasonSubmittedAt
	) {
		return new MemberWeekProgress(
			PROGRESS_ID,
			WEEK_ID,
			OWNER_MEMBER_ID,
			status,
			startedAt,
			WEEK_DUE_AT,
			completedAt,
			completionNote,
			incompleteReason,
			reasonSubmittedAt,
			NOW,
			NOW
		);
	}

	private static CurriculumGeneration generation() {
		return new CurriculumGeneration(
			"Spring Boot 6주 완성",
			List.of(new CurriculumWeekPlan(
				1,
				"JPA 기초와 환경 구성",
				"공통 환경을 만들고 핵심 개념을 맞춥니다.",
				List.of("Entity 매핑 이해"),
				List.of(Map.of("title", "공식 문서", "url", "https://spring.io/projects/spring-boot")),
				List.of(new CurriculumTaskPlan(
					WeeklyTaskType.READING,
					"JPA 엔티티 매핑 읽기",
					"핵심 매핑 규칙을 정리합니다.",
					true
				))
			)),
			"Generate a curriculum as JSON.",
			LlmProvider.OPENAI,
			"gpt-4o-mini",
			12,
			34,
			BigDecimal.ZERO,
			250,
			LlmUsageStatus.SUCCESS,
			null,
			Map.of("purpose", "CURRICULUM_GENERATE"),
			"Generated one week with one task."
		);
	}

	private static final class CapturingRepository implements CurriculumRepository {

		private boolean groupExists;
		private CurriculumStartContext startContext;
		private CurriculumStartContext readContext;
		private CurriculumStartContext weekReadContext;
		private CurriculumStartContext taskReadContext;
		private List<SubmittedOnboardingResponse> submittedResponses = List.of();
		private Curriculum activeCurriculum;
		private CurriculumWeek currentWeek;
		private List<WeeklyTask> weeklyTasks = List.of();
		private WeeklyTask weeklyTask;
		private MemberWeekProgress existingProgress;
		private MemberWeekProgress raceProgress;
		private TaskCompletion existingTaskCompletion;
		private TaskCompletion raceTaskCompletion;
		private Instant weekDueAt = WEEK_DUE_AT;
		private boolean insertSucceeds = true;
		private boolean insertTaskCompletionSucceeds = true;
		private CurriculumGenerationRequest generationRequest;
		private boolean weekExists;
		private boolean taskExists;
		private UUID savedGroupId;
		private Instant savedStartedAt;
		private com.studypot.aistudyleader.llm.domain.LlmUsage savedLlmUsage;
		private Curriculum savedCurriculum;
		private MemberWeekProgress insertedProgress;
		private MemberWeekProgress updatedProgress;
		private TaskCompletion insertedTaskCompletion;
		private TaskCompletion updatedTaskCompletion;

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			return groupExists || startContext != null || readContext != null;
		}

		@Override
		public Optional<CurriculumStartContext> findStartContext(UUID groupId, UUID userId) {
			return Optional.ofNullable(startContext);
		}

		@Override
		public List<SubmittedOnboardingResponse> findSubmittedOnboardingResponses(UUID groupId) {
			return submittedResponses;
		}

		@Override
		public void saveStartedCurriculum(UUID groupId, Instant startedAt, com.studypot.aistudyleader.llm.domain.LlmUsage llmUsage, Curriculum curriculum) {
			this.savedGroupId = groupId;
			this.savedStartedAt = startedAt;
			this.savedLlmUsage = llmUsage;
			this.savedCurriculum = curriculum;
		}

		@Override
		public Optional<CurriculumStartContext> findReadContext(UUID groupId, UUID userId) {
			return Optional.ofNullable(readContext);
		}

		@Override
		public Optional<Curriculum> findActiveCurriculumByGroupId(UUID groupId) {
			return Optional.ofNullable(activeCurriculum);
		}

		@Override
		public Optional<CurriculumWeek> findCurrentWeekByGroupId(UUID groupId) {
			return Optional.ofNullable(currentWeek);
		}

		@Override
		public boolean existsCurriculumWeek(UUID weekId) {
			return weekExists;
		}

		@Override
		public Optional<CurriculumStartContext> findReadContextByWeekId(UUID weekId, UUID userId) {
			return Optional.ofNullable(weekReadContext);
		}

		@Override
		public Optional<CurriculumStartContext> findReadContextByTaskId(UUID taskId, UUID userId) {
			return Optional.ofNullable(taskReadContext);
		}

		@Override
		public List<WeeklyTask> findWeeklyTasksByWeekId(UUID weekId) {
			return weeklyTasks;
		}

		@Override
		public boolean existsWeeklyTask(UUID taskId) {
			return taskExists;
		}

		@Override
		public Optional<WeeklyTask> findWeeklyTaskById(UUID taskId) {
			return Optional.ofNullable(weeklyTask);
		}

		@Override
		public Optional<MemberWeekProgress> findMemberWeekProgress(UUID weekId, UUID memberId) {
			return Optional.ofNullable(existingProgress);
		}

		@Override
		public Optional<Instant> findWeekDueAt(UUID weekId) {
			return Optional.ofNullable(weekDueAt);
		}

		@Override
		public boolean insertMemberWeekProgress(MemberWeekProgress progress) {
			if (!insertSucceeds) {
				existingProgress = raceProgress;
				return false;
			}
			insertedProgress = progress;
			existingProgress = progress;
			return true;
		}

		@Override
		public boolean updateMemberWeekProgress(MemberWeekProgress progress) {
			updatedProgress = progress;
			existingProgress = progress;
			return true;
		}

		@Override
		public Optional<TaskCompletion> findTaskCompletion(UUID taskId, UUID memberId) {
			return Optional.ofNullable(existingTaskCompletion);
		}

		@Override
		public boolean insertTaskCompletion(TaskCompletion completion) {
			if (!insertTaskCompletionSucceeds) {
				existingTaskCompletion = raceTaskCompletion;
				return false;
			}
			insertedTaskCompletion = completion;
			existingTaskCompletion = completion;
			return true;
		}

		@Override
		public boolean updateTaskCompletion(TaskCompletion completion) {
			updatedTaskCompletion = completion;
			existingTaskCompletion = completion;
			return true;
		}
	}
}
