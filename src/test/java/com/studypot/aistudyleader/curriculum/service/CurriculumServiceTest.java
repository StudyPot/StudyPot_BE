package com.studypot.aistudyleader.curriculum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.curriculum.domain.Curriculum;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGeneration;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGenerationRequest;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStartContext;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStatus;
import com.studypot.aistudyleader.curriculum.domain.CurriculumTaskPlan;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeekPlan;
import com.studypot.aistudyleader.curriculum.domain.LlmProvider;
import com.studypot.aistudyleader.curriculum.domain.LlmUsageStatus;
import com.studypot.aistudyleader.curriculum.domain.SubmittedAvailabilitySlot;
import com.studypot.aistudyleader.curriculum.domain.SubmittedOnboardingResponse;
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
	private static final Instant NOW = Instant.parse("2026-05-11T01:15:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Test
	void startStudyCreatesCurriculumFromSubmittedOnboardingAndActivatesGroup() {
		CapturingRepository repository = new CapturingRepository();
		repository.startContext = ownerStartContext(StudyGroupStatus.ONBOARDING);
		repository.submittedResponses = List.of(submittedResponse());
		CurriculumService service = service(repository, generation(), LLM_USAGE_ID, CURRICULUM_ID, WEEK_ID, TASK_ID);

		Curriculum result = service.startStudy(new StartCurriculumCommand(USER_ID, GROUP_ID));

		assertThat(repository.savedGroupId).isEqualTo(GROUP_ID);
		assertThat(repository.savedStartedAt).isEqualTo(NOW);
		assertThat(repository.savedLlmUsage.id()).isEqualTo(LLM_USAGE_ID);
		assertThat(repository.savedLlmUsage.purpose()).isEqualTo("CURRICULUM_GENERATE");
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
		repository.startContext = ownerStartContext(StudyGroupStatus.ACTIVE);
		CurriculumService service = service(repository, generation(), LLM_USAGE_ID, CURRICULUM_ID, WEEK_ID, TASK_ID);

		assertThatThrownBy(() -> service.startStudy(new StartCurriculumCommand(USER_ID, GROUP_ID)))
			.isInstanceOf(CurriculumStartRejectedException.class)
			.hasMessage("study group must be ONBOARDING to start curriculum generation.");
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

	private static CurriculumStartContext ownerStartContext(StudyGroupStatus status) {
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
			GroupMemberStatus.PENDING_ONBOARDING
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
		private List<SubmittedOnboardingResponse> submittedResponses = List.of();
		private Curriculum activeCurriculum;
		private CurriculumGenerationRequest generationRequest;
		private UUID savedGroupId;
		private Instant savedStartedAt;
		private com.studypot.aistudyleader.curriculum.domain.LlmUsage savedLlmUsage;
		private Curriculum savedCurriculum;

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
		public void saveStartedCurriculum(UUID groupId, Instant startedAt, com.studypot.aistudyleader.curriculum.domain.LlmUsage llmUsage, Curriculum curriculum) {
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
	}
}
