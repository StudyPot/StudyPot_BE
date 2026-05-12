package com.studypot.aistudyleader.curriculum.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.curriculum.domain.Curriculum;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGeneration;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStartContext;
import com.studypot.aistudyleader.curriculum.domain.CurriculumTaskPlan;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeek;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeekPlan;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeekStatus;
import com.studypot.aistudyleader.curriculum.domain.LlmProvider;
import com.studypot.aistudyleader.curriculum.domain.LlmUsage;
import com.studypot.aistudyleader.curriculum.domain.LlmUsageStatus;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgress;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgressStatus;
import com.studypot.aistudyleader.curriculum.domain.SubmittedOnboardingResponse;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTask;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import com.studypot.aistudyleader.curriculum.repository.CurriculumRepository;
import com.studypot.aistudyleader.curriculum.service.CurriculumService;
import com.studypot.aistudyleader.global.api.ApiPaths;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(classes = {AiStudyLeaderApplication.class, CurriculumControllerTest.TestCurriculumBeans.class})
@AutoConfigureMockMvc
class CurriculumControllerTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000004121");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000004122");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000004123");
	private static final UUID LLM_USAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000004124");
	private static final UUID CURRICULUM_ID = UUID.fromString("018f0000-0000-7000-8000-000000004125");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000004126");
	private static final UUID TASK_ID = UUID.fromString("018f0000-0000-7000-8000-000000004127");
	private static final UUID PRACTICE_TASK_ID = UUID.fromString("018f0000-0000-7000-8000-000000004129");
	private static final UUID ASSIGNMENT_TASK_ID = UUID.fromString("018f0000-0000-7000-8000-000000004130");
	private static final UUID PROJECT_TASK_ID = UUID.fromString("018f0000-0000-7000-8000-000000004131");
	private static final UUID CUSTOM_TASK_ID = UUID.fromString("018f0000-0000-7000-8000-000000004132");
	private static final UUID PROGRESS_ID = UUID.fromString("018f0000-0000-7000-8000-000000004133");
	private static final String START_PATH = ApiPaths.V1 + "/groups/" + GROUP_ID + "/start";
	private static final String CURRICULUM_PATH = ApiPaths.V1 + "/groups/" + GROUP_ID + "/curriculum";
	private static final String CURRENT_WEEK_PATH = ApiPaths.V1 + "/groups/" + GROUP_ID + "/weeks/current";
	private static final String WEEK_TASKS_PATH = ApiPaths.V1 + "/weeks/" + WEEK_ID + "/tasks";
	private static final String WEEK_PROGRESS_PATH = ApiPaths.V1 + "/weeks/" + WEEK_ID + "/progress/me";

	private final MockMvc mockMvc;
	private final MutableCurriculumRepository repository;

	@Autowired
	CurriculumControllerTest(MockMvc mockMvc, MutableCurriculumRepository repository) {
		this.mockMvc = mockMvc;
		this.repository = repository;
	}

	@BeforeEach
	void setUp() {
		repository.reset();
	}

	@Test
	void startStudyRequiresAuthentication() throws Exception {
		mockMvc.perform(post(START_PATH)
				.with(xsrf("start-xsrf")))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void startStudyReturnsCreatedCurriculum() throws Exception {
		mockMvc.perform(post(START_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("start-xsrf")))
			.andExpect(status().isCreated())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(CURRICULUM_ID.toString()))
			.andExpect(jsonPath("$.groupId").value(GROUP_ID.toString()))
			.andExpect(jsonPath("$.title").value("Spring Boot 6주 완성"))
			.andExpect(jsonPath("$.totalWeeks").value(1))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.onboardingSummary.submittedResponseCount").value(1));
	}

	@Test
	void startStudyReturnsForbiddenForNonOwner() throws Exception {
		repository.startContext = context(StudyGroupStatus.ONBOARDING, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);

		mockMvc.perform(post(START_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("start-xsrf")))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Forbidden"));
	}

	@Test
	void startStudyReturnsConflictForAlreadyActiveGroup() throws Exception {
		repository.startContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.OWNER, GroupMemberStatus.ACTIVE);

		mockMvc.perform(post(START_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("start-xsrf")))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Conflict"));
	}

	@Test
	void startStudyReturnsConflictWhenOwnerOnboardingIsPending() throws Exception {
		repository.startContext = context(StudyGroupStatus.ONBOARDING, GroupMemberPermission.OWNER, GroupMemberStatus.PENDING_ONBOARDING);

		mockMvc.perform(post(START_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("start-xsrf")))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("owner onboarding must be submitted before starting the study."));
	}

	@Test
	void getCurriculumReturnsActiveCurriculum() throws Exception {
		repository.activeCurriculum = generation().toCurriculum(
			CURRICULUM_ID,
			GROUP_ID,
			LLM_USAGE_ID,
			Map.of("submittedResponseCount", 1),
			TestCurriculumBeans.NOW,
			List.of(WEEK_ID),
			List.of(TASK_ID)
		);

		mockMvc.perform(get(CURRICULUM_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(CURRICULUM_ID.toString()))
			.andExpect(jsonPath("$.groupId").value(GROUP_ID.toString()))
			.andExpect(jsonPath("$.title").value("Spring Boot 6주 완성"))
			.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	void getCurrentWeekRequiresAuthentication() throws Exception {
		mockMvc.perform(get(CURRENT_WEEK_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void getCurrentWeekReturnsInProgressWeek() throws Exception {
		repository.currentWeek = currentWeek();

		mockMvc.perform(get(CURRENT_WEEK_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(WEEK_ID.toString()))
			.andExpect(jsonPath("$.curriculumId").value(CURRICULUM_ID.toString()))
			.andExpect(jsonPath("$.weekNumber").value(1))
			.andExpect(jsonPath("$.title").value("JPA 기초와 환경 구성"))
			.andExpect(jsonPath("$.sprintGoal").value("Entity 매핑 이해"))
			.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
			.andExpect(jsonPath("$.startsAt").value("2026-05-11T02:20:00Z"))
			.andExpect(jsonPath("$.endsAt").value("2026-05-18T02:20:00Z"));
	}

	@Test
	void getCurrentWeekReturnsForbiddenForPendingMember() throws Exception {
		repository.readContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.PENDING_ONBOARDING);

		mockMvc.perform(get(CURRENT_WEEK_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Forbidden"));
	}

	@Test
	void listWeeklyTasksRequiresAuthentication() throws Exception {
		mockMvc.perform(get(WEEK_TASKS_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void listWeeklyTasksReturnsTasksInDisplayOrder() throws Exception {
		repository.weekExists = true;
		repository.weekReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
		repository.weeklyTasks = List.of(
			task(TASK_ID, 1, WeeklyTaskType.READING, true),
			task(PRACTICE_TASK_ID, 2, WeeklyTaskType.PRACTICE, true),
			task(ASSIGNMENT_TASK_ID, 3, WeeklyTaskType.ASSIGNMENT, true),
			task(PROJECT_TASK_ID, 4, WeeklyTaskType.PROJECT, false),
			task(CUSTOM_TASK_ID, 5, WeeklyTaskType.CUSTOM, false)
		);

		mockMvc.perform(get(WEEK_TASKS_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$[0].id").value(TASK_ID.toString()))
			.andExpect(jsonPath("$[0].curriculumWeekId").value(WEEK_ID.toString()))
			.andExpect(jsonPath("$[0].displayOrder").value(1))
			.andExpect(jsonPath("$[0].taskType").value("READING"))
			.andExpect(jsonPath("$[0].title").value("READING task"))
			.andExpect(jsonPath("$[0].description").value("READING description"))
			.andExpect(jsonPath("$[0].required").value(true))
			.andExpect(jsonPath("$[0].dueAt").value("2026-05-18T02:20:00Z"))
			.andExpect(jsonPath("$[1].taskType").value("PRACTICE"))
			.andExpect(jsonPath("$[2].taskType").value("ASSIGNMENT"))
			.andExpect(jsonPath("$[3].taskType").value("PROJECT"))
			.andExpect(jsonPath("$[4].taskType").value("CUSTOM"));
	}

	@Test
	void updateMyWeekProgressRequiresAuthentication() throws Exception {
		mockMvc.perform(put(WEEK_PROGRESS_PATH)
				.with(xsrf("progress-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void updateMyWeekProgressCreatesProgressForActiveMember() throws Exception {
		repository.weekExists = true;
		repository.weekReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
		repository.nextIds = new ArrayDeque<>(List.of(PROGRESS_ID));

		mockMvc.perform(put(WEEK_PROGRESS_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("progress-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"status":"IN_PROGRESS"}
					"""))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(PROGRESS_ID.toString()))
			.andExpect(jsonPath("$.status").value("IN_PROGRESS"));
	}

	@Test
	void updateMyWeekProgressReturnsForbiddenForPendingMember() throws Exception {
		repository.weekExists = true;
		repository.weekReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.PENDING_ONBOARDING);

		mockMvc.perform(put(WEEK_PROGRESS_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("progress-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"status":"IN_PROGRESS"}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Forbidden"));
	}

	@Test
	void updateMyWeekProgressReturnsValidationProblemForIncompleteWithoutReason() throws Exception {
		repository.weekExists = true;
		repository.weekReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
		repository.nextIds = new ArrayDeque<>(List.of(PROGRESS_ID));

		mockMvc.perform(put(WEEK_PROGRESS_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("progress-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"status":"INCOMPLETE"}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void updateMyWeekProgressReturnsValidationProblemWhenStatusIsMissing() throws Exception {
		mockMvc.perform(put(WEEK_PROGRESS_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("progress-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	private static RequestPostProcessor xsrf(String value) {
		return request -> {
			jakarta.servlet.http.Cookie[] existingCookies = request.getCookies();
			jakarta.servlet.http.Cookie[] cookies = existingCookies == null
				? new jakarta.servlet.http.Cookie[1]
				: java.util.Arrays.copyOf(existingCookies, existingCookies.length + 1);
			cookies[cookies.length - 1] = new MockCookie("XSRF-TOKEN", value);
			request.setCookies(cookies);
			request.addHeader("X-XSRF-TOKEN", value);
			return request;
		};
	}

	private static CurriculumStartContext context(
		StudyGroupStatus groupStatus,
		GroupMemberPermission permission,
		GroupMemberStatus memberStatus
	) {
		return new CurriculumStartContext(
			GROUP_ID,
			"Backend Interview Study",
			"Spring Boot",
			List.of("JPA", "Security"),
			groupStatus,
			LocalDate.parse("2026-05-11"),
			LocalDate.parse("2026-06-21"),
			MEMBER_ID,
			permission,
			memberStatus
		);
	}

	private static CurriculumGeneration generation() {
		return new CurriculumGeneration(
			"Spring Boot 6주 완성",
			List.of(new CurriculumWeekPlan(
				1,
				"JPA 기초와 환경 구성",
				"핵심 개념을 맞춥니다.",
				List.of("Entity 매핑 이해"),
				List.of(),
				List.of(new CurriculumTaskPlan(WeeklyTaskType.READING, "JPA 읽기", null, true))
			)),
			"Generate a curriculum as JSON.",
			LlmProvider.OPENAI,
			"gpt-4o-mini",
			10,
			20,
			BigDecimal.ZERO,
			100,
			LlmUsageStatus.SUCCESS,
			null,
			Map.of("purpose", "CURRICULUM_GENERATE"),
			"Generated."
		);
	}

	private static CurriculumWeek currentWeek() {
		return new CurriculumWeek(
			WEEK_ID,
			CURRICULUM_ID,
			1,
			"JPA 기초와 환경 구성",
			"핵심 개념을 맞춥니다.",
			"Entity 매핑 이해",
			List.of("Entity 매핑 이해"),
			List.of(),
			CurriculumWeekStatus.IN_PROGRESS,
			TestCurriculumBeans.NOW,
			TestCurriculumBeans.NOW.plusSeconds(604800),
			List.of(task(TASK_ID, 1, WeeklyTaskType.READING, true)),
			TestCurriculumBeans.NOW,
			TestCurriculumBeans.NOW
		);
	}

	private static WeeklyTask task(UUID id, int displayOrder, WeeklyTaskType taskType, boolean required) {
		return new WeeklyTask(
			id,
			WEEK_ID,
			displayOrder,
			taskType,
			taskType.name() + " task",
			taskType.name() + " description",
			required,
			TestCurriculumBeans.NOW.plusSeconds(604800),
			true,
			Map.of("displayOrder", displayOrder),
			TestCurriculumBeans.NOW,
			TestCurriculumBeans.NOW
		);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestCurriculumBeans {

		private static final Instant NOW = Instant.parse("2026-05-11T02:20:00Z");

		@Bean
		@Primary
		MutableCurriculumRepository mutableCurriculumRepository() {
			return new MutableCurriculumRepository();
		}

		@Bean
		@Primary
		CurriculumService curriculumService(MutableCurriculumRepository repository) {
			return new CurriculumService(
				repository,
				request -> generation(),
				Clock.fixed(NOW, ZoneOffset.UTC),
				repository::nextId
			);
		}
	}

	static final class MutableCurriculumRepository implements CurriculumRepository {

		private CurriculumStartContext startContext;
		private CurriculumStartContext readContext;
		private CurriculumStartContext weekReadContext;
		private Curriculum activeCurriculum;
		private CurriculumWeek currentWeek;
		private boolean weekExists;
		private List<WeeklyTask> weeklyTasks;
		private MemberWeekProgress progress;
		private Instant weekDueAt;
		private Queue<UUID> nextIds;

		void reset() {
			startContext = context(StudyGroupStatus.ONBOARDING, GroupMemberPermission.OWNER, GroupMemberStatus.ACTIVE);
			readContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.OWNER, GroupMemberStatus.ACTIVE);
			weekReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
			activeCurriculum = null;
			currentWeek = null;
			weekExists = false;
			weeklyTasks = List.of();
			progress = null;
			weekDueAt = TestCurriculumBeans.NOW.plusSeconds(604800);
			nextIds = new ArrayDeque<>(List.of(LLM_USAGE_ID, CURRICULUM_ID, WEEK_ID, TASK_ID));
		}

		UUID nextId() {
			UUID id = nextIds.poll();
			if (id == null) {
				throw new IllegalStateException("No test UUID left.");
			}
			return id;
		}

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			return true;
		}

		@Override
		public Optional<CurriculumStartContext> findStartContext(UUID groupId, UUID userId) {
			return Optional.ofNullable(startContext);
		}

		@Override
		public List<SubmittedOnboardingResponse> findSubmittedOnboardingResponses(UUID groupId) {
			return List.of(new SubmittedOnboardingResponse(
				UUID.fromString("018f0000-0000-7000-8000-000000004128"),
				MEMBER_ID,
				Map.of("JPA", 2),
				Map.of("READING", 4),
				null,
				List.of(),
				Instant.parse("2026-05-10T08:00:00Z")
			));
		}

		@Override
		public void saveStartedCurriculum(UUID groupId, Instant startedAt, LlmUsage llmUsage, Curriculum curriculum) {
			this.activeCurriculum = curriculum;
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
		public List<WeeklyTask> findWeeklyTasksByWeekId(UUID weekId) {
			return weeklyTasks;
		}

		@Override
		public Optional<MemberWeekProgress> findMemberWeekProgress(UUID weekId, UUID memberId) {
			return Optional.ofNullable(progress);
		}

		@Override
		public Optional<Instant> findWeekDueAt(UUID weekId) {
			return Optional.ofNullable(weekDueAt);
		}

		@Override
		public boolean insertMemberWeekProgress(MemberWeekProgress progress) {
			this.progress = progress;
			return true;
		}

		@Override
		public boolean updateMemberWeekProgress(MemberWeekProgress progress) {
			this.progress = progress;
			return true;
		}
	}
}
