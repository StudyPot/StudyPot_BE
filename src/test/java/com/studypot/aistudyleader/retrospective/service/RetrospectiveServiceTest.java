package com.studypot.aistudyleader.retrospective.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgressStatus;
import com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import com.studypot.aistudyleader.retrospective.domain.Retrospective;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
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

	private static final class CapturingRepository implements RetrospectiveRepository {

		private boolean weekExists;
		private RetrospectiveMembershipContext membership;
		private RetrospectiveProgress progress;
		private Retrospective existingRetrospective;
		private Retrospective retrospectiveById;
		private List<RetrospectiveTaskSummary> taskSummaries = List.of();
		private Retrospective insertedRetrospective;
		private Retrospective updatedRetrospective;

		@Override
		public boolean existsCurriculumWeek(UUID weekId) {
			return weekExists;
		}

		@Override
		public Optional<RetrospectiveMembershipContext> findMembershipByWeekId(UUID weekId, UUID userId) {
			return Optional.ofNullable(membership);
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
			return true;
		}
	}
}
