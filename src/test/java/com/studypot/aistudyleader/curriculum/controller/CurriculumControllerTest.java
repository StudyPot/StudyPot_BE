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
import com.studypot.aistudyleader.curriculum.domain.CurriculumSprintPlanner;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStartContext;
import com.studypot.aistudyleader.curriculum.domain.CurriculumTaskPlan;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeek;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeekPlan;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeekStatus;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsage;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgress;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgressStatus;
import com.studypot.aistudyleader.curriculum.domain.SubmittedOnboardingResponse;
import com.studypot.aistudyleader.curriculum.domain.GroupActivityCount;
import com.studypot.aistudyleader.curriculum.domain.TaskCompletion;
import com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus;
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
	private static final UUID COMPLETION_ID = UUID.fromString("018f0000-0000-7000-8000-000000004134");
	private static final String START_PATH = ApiPaths.V1 + "/groups/" + GROUP_ID + "/start";
	private static final String CURRICULUM_PATH = ApiPaths.V1 + "/groups/" + GROUP_ID + "/curriculum";
	private static final String CURRENT_WEEK_PATH = ApiPaths.V1 + "/groups/" + GROUP_ID + "/weeks/current";
	private static final String LEARNING_ACTIVITY_PATH = ApiPaths.V1 + "/groups/" + GROUP_ID + "/learning-activity/me";
	private static final String WEEK_TASKS_PATH = ApiPaths.V1 + "/weeks/" + WEEK_ID + "/tasks";
	private static final String WEEK_PROGRESS_PATH = ApiPaths.V1 + "/weeks/" + WEEK_ID + "/progress/me";
	private static final String TASK_COMPLETION_PATH = ApiPaths.V1 + "/tasks/" + TASK_ID + "/completion/me";
	private static final String TASK_DONE_PATH = TASK_COMPLETION_PATH + "/done";
	private static final String TASK_INCOMPLETE_PATH = TASK_COMPLETION_PATH + "/incomplete";
	private static final String TASK_SKIP_PATH = TASK_COMPLETION_PATH + "/skip";

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
		repository.startContext = context(StudyGroupStatus.READY_TO_START, GroupMemberPermission.OWNER, GroupMemberStatus.PENDING_ONBOARDING);

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
			CurriculumSprintPlanner.fixedWeeklyWindows(LocalDate.parse("2026-05-11"), LocalDate.parse("2026-05-17")),
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
	void listCurriculumWeeksReturnsWeeks() throws Exception {
		repository.currentWeek = currentWeek();

		mockMvc.perform(get(ApiPaths.V1 + "/groups/" + GROUP_ID + "/weeks")
				.with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$[0].id").value(WEEK_ID.toString()))
			.andExpect(jsonPath("$[0].weekNumber").value(1));
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
	void listWeeklyTasksIncludesMyCompletionStatus() throws Exception {
		repository.weekExists = true;
		repository.weekReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
		repository.weeklyTasks = List.of(
			task(TASK_ID, 1, WeeklyTaskType.READING, true),
			task(PRACTICE_TASK_ID, 2, WeeklyTaskType.PRACTICE, true)
		);
		repository.taskCompletions = List.of(completion(
			COMPLETION_ID,
			TASK_ID,
			TaskCompletionStatus.SKIPPED,
			null,
			null,
			null,
			null,
			null
		));

		mockMvc.perform(get(WEEK_TASKS_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(TASK_ID.toString()))
			.andExpect(jsonPath("$[0].completion.taskId").value(TASK_ID.toString()))
			.andExpect(jsonPath("$[0].completion.status").value("SKIPPED"))
			.andExpect(jsonPath("$[1].id").value(PRACTICE_TASK_ID.toString()))
			.andExpect(jsonPath("$[1].completion").doesNotExist());
	}

	@Test
	void getLearningActivityRequiresAuthentication() throws Exception {
		mockMvc.perform(get(LEARNING_ACTIVITY_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void getActivityHeatmapRequiresAuthentication() throws Exception {
		mockMvc.perform(get(ApiPaths.V1 + "/groups/" + GROUP_ID + "/activity-heatmap"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void getActivityHeatmapReturnsMemberDailyCounts() throws Exception {
		repository.groupActivityCounts = List.of(
			new GroupActivityCount(MEMBER_ID, USER_ID, "현우", "hyunwoo", LocalDate.parse("2026-05-11"), 3),
			new GroupActivityCount(MEMBER_ID, USER_ID, "현우", "hyunwoo", LocalDate.parse("2026-05-12"), 1)
		);

		mockMvc.perform(get(ApiPaths.V1 + "/groups/" + GROUP_ID + "/activity-heatmap")
				.with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.startDate").value("2026-05-11"))
			.andExpect(jsonPath("$.endDate").value("2026-05-17"))
			.andExpect(jsonPath("$.days.length()").value(7))
			.andExpect(jsonPath("$.members[0].userId").value(USER_ID.toString()))
			.andExpect(jsonPath("$.members[0].displayName").value("현우"))
			.andExpect(jsonPath("$.members[0].nickname").value("hyunwoo"))
			.andExpect(jsonPath("$.members[0].counts.length()").value(7))
			.andExpect(jsonPath("$.members[0].counts[0]").value(3))
			.andExpect(jsonPath("$.members[0].counts[1]").value(1));
	}

	@Test
	void getGroupMembersActivityReturnsDailyActivityRows() throws Exception {
		repository.groupActivityCounts = List.of(
			new GroupActivityCount(MEMBER_ID, USER_ID, "현우", "hyunwoo", LocalDate.parse("2026-05-11"), 3),
			new GroupActivityCount(MEMBER_ID, USER_ID, "현우", "hyunwoo", LocalDate.parse("2026-05-12"), 1)
		);

		mockMvc.perform(get(ApiPaths.V1 + "/groups/" + GROUP_ID + "/learning-activity")
				.with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$[0].memberId").value(MEMBER_ID.toString()))
			.andExpect(jsonPath("$[0].memberNickname").value("hyunwoo"))
			.andExpect(jsonPath("$[0].dailyActivity.length()").value(7))
			.andExpect(jsonPath("$[0].dailyActivity[0].date").value("2026-05-11"))
			.andExpect(jsonPath("$[0].dailyActivity[0].count").value(3))
			.andExpect(jsonPath("$[0].dailyActivity[1].count").value(1));
	}

	@Test
	void getLearningActivityReturnsCurrentWeekTasksAndMyCompletionState() throws Exception {
		repository.currentWeek = new CurriculumWeek(
			WEEK_ID,
			CURRICULUM_ID,
			1,
			"JPA 기초와 환경 구성",
			"핵심 개념을 맞춥니다.",
			"Entity 매핑 이해",
			java.util.List.of(),
			List.of("Entity 매핑 이해"),
			List.of(),
			CurriculumWeekStatus.IN_PROGRESS,
			TestCurriculumBeans.NOW,
			TestCurriculumBeans.NOW.plusSeconds(604800),
			List.of(
				task(TASK_ID, 1, WeeklyTaskType.READING, true),
				task(PRACTICE_TASK_ID, 2, WeeklyTaskType.PRACTICE, true)
			),
			TestCurriculumBeans.NOW,
			TestCurriculumBeans.NOW
		);
		repository.progress = progress(MemberWeekProgressStatus.IN_PROGRESS, TestCurriculumBeans.NOW.minusSeconds(60), null, null, null, null);
		repository.taskCompletions = List.of(completion(
			COMPLETION_ID,
			TASK_ID,
			TaskCompletionStatus.DONE,
			TestCurriculumBeans.NOW,
			"정리 완료",
			null,
			null,
			"https://example.com/evidence"
		));

		mockMvc.perform(get(LEARNING_ACTIVITY_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.groupId").value(GROUP_ID.toString()))
			.andExpect(jsonPath("$.currentWeek.id").value(WEEK_ID.toString()))
			.andExpect(jsonPath("$.currentWeek.weekNumber").value(1))
			.andExpect(jsonPath("$.progress.id").value(PROGRESS_ID.toString()))
			.andExpect(jsonPath("$.progressStatus").value("IN_PROGRESS"))
			.andExpect(jsonPath("$.taskCompletion.totalCount").value(2))
			.andExpect(jsonPath("$.taskCompletion.doneCount").value(1))
			.andExpect(jsonPath("$.taskCompletion.incompleteCount").value(0))
			.andExpect(jsonPath("$.taskCompletion.skippedCount").value(0))
			.andExpect(jsonPath("$.tasks[0].task.id").value(TASK_ID.toString()))
			.andExpect(jsonPath("$.tasks[0].completion.id").value(COMPLETION_ID.toString()))
			.andExpect(jsonPath("$.tasks[0].completion.taskId").value(TASK_ID.toString()))
			.andExpect(jsonPath("$.tasks[0].completion.status").value("DONE"))
			.andExpect(jsonPath("$.tasks[0].completion.completionNote").value("정리 완료"))
			.andExpect(jsonPath("$.tasks[1].task.id").value(PRACTICE_TASK_ID.toString()))
			.andExpect(jsonPath("$.tasks[1].completion.id").doesNotExist())
			.andExpect(jsonPath("$.tasks[1].completion.taskId").value(PRACTICE_TASK_ID.toString()))
			.andExpect(jsonPath("$.tasks[1].completion.status").value("TODO"));
	}

	@Test
	void getLearningActivityDefaultsProgressStatusWhenProgressIsMissing() throws Exception {
		repository.currentWeek = currentWeek();

		mockMvc.perform(get(LEARNING_ACTIVITY_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.progress").doesNotExist())
			.andExpect(jsonPath("$.progressStatus").value("NOT_STARTED"))
			.andExpect(jsonPath("$.tasks[0].completion.status").value("TODO"));
	}

	@Test
	void getLearningActivityReturnsForbiddenForPendingMember() throws Exception {
		repository.readContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.PENDING_ONBOARDING);
		repository.currentWeek = currentWeek();

		mockMvc.perform(get(LEARNING_ACTIVITY_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Forbidden"));
	}

	@Test
	void getMyWeekProgressRequiresAuthentication() throws Exception {
		mockMvc.perform(get(WEEK_PROGRESS_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void getMyWeekProgressReturnsExistingProgress() throws Exception {
		repository.weekExists = true;
		repository.weekReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
		repository.progress = progress(
			MemberWeekProgressStatus.INCOMPLETE,
			TestCurriculumBeans.NOW.minusSeconds(60),
			null,
			null,
			"실습을 완료하지 못했습니다.",
			TestCurriculumBeans.NOW
		);

		mockMvc.perform(get(WEEK_PROGRESS_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(PROGRESS_ID.toString()))
			.andExpect(jsonPath("$.status").value("INCOMPLETE"))
			.andExpect(jsonPath("$.completedAt").doesNotExist())
			.andExpect(jsonPath("$.incompleteReason").value("실습을 완료하지 못했습니다."));
	}

	@Test
	void getMyWeekProgressReturnsNotFoundWhenProgressIsMissing() throws Exception {
		repository.weekExists = true;
		repository.weekReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);

		mockMvc.perform(get(WEEK_PROGRESS_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Not Found"));
	}

	@Test
	void getMyWeekProgressReturnsForbiddenForPendingMember() throws Exception {
		repository.weekExists = true;
		repository.weekReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.PENDING_ONBOARDING);
		repository.progress = progress(MemberWeekProgressStatus.IN_PROGRESS, TestCurriculumBeans.NOW, null, null, null, null);

		mockMvc.perform(get(WEEK_PROGRESS_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Forbidden"));
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

	@Test
	void completeTaskRequiresAuthentication() throws Exception {
		mockMvc.perform(post(TASK_COMPLETION_PATH)
				.with(xsrf("task-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"status":"DONE"}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void completeTaskReturnsDoneCompletion() throws Exception {
		repository.taskExists = true;
		repository.taskReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
		repository.weeklyTask = task(TASK_ID, 1, WeeklyTaskType.READING, true, TestCurriculumBeans.NOW.plusSeconds(3600));
		repository.progress = progress(MemberWeekProgressStatus.IN_PROGRESS, TestCurriculumBeans.NOW.minusSeconds(60), null, null, null, null);
		repository.nextIds = new ArrayDeque<>(List.of(COMPLETION_ID));

		mockMvc.perform(post(TASK_COMPLETION_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("task-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"status":"DONE","completionNote":"정리 완료","evidenceUrl":"https://example.com/evidence"}
					"""))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(COMPLETION_ID.toString()))
			.andExpect(jsonPath("$.taskId").value(TASK_ID.toString()))
			.andExpect(jsonPath("$.status").value("DONE"))
			.andExpect(jsonPath("$.completedAt").value("2026-05-11T02:20:00Z"))
			.andExpect(jsonPath("$.reasonSubmittedAt").doesNotExist())
			.andExpect(jsonPath("$.completionNote").value("정리 완료"))
			.andExpect(jsonPath("$.incompleteReason").doesNotExist())
			.andExpect(jsonPath("$.evidenceUrl").value("https://example.com/evidence"));
	}

	@Test
	void completeTaskDoneActionReturnsDoneCompletion() throws Exception {
		repository.taskExists = true;
		repository.taskReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
		repository.weeklyTask = task(TASK_ID, 1, WeeklyTaskType.READING, true, TestCurriculumBeans.NOW.plusSeconds(3600));
		repository.progress = progress(MemberWeekProgressStatus.IN_PROGRESS, TestCurriculumBeans.NOW.minusSeconds(60), null, null, null, null);
		repository.nextIds = new ArrayDeque<>(List.of(COMPLETION_ID));

		mockMvc.perform(post(TASK_DONE_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("task-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"completionNote":"정리 완료","evidenceUrl":"https://example.com/evidence"}
					"""))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(COMPLETION_ID.toString()))
			.andExpect(jsonPath("$.taskId").value(TASK_ID.toString()))
			.andExpect(jsonPath("$.status").value("DONE"))
			.andExpect(jsonPath("$.completionNote").value("정리 완료"))
			.andExpect(jsonPath("$.evidenceUrl").value("https://example.com/evidence"));
	}

	@Test
	void completeTaskReturnsIncompleteCompletion() throws Exception {
		repository.taskExists = true;
		repository.taskReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
		repository.weeklyTask = task(TASK_ID, 1, WeeklyTaskType.ASSIGNMENT, true, TestCurriculumBeans.NOW.minusSeconds(60));
		repository.progress = progress(MemberWeekProgressStatus.IN_PROGRESS, TestCurriculumBeans.NOW.minusSeconds(3600), null, null, null, null);
		repository.nextIds = new ArrayDeque<>(List.of(COMPLETION_ID));

		mockMvc.perform(post(TASK_COMPLETION_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("task-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"status":"INCOMPLETE","incompleteReason":"실습을 끝내지 못했습니다."}
					"""))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(COMPLETION_ID.toString()))
			.andExpect(jsonPath("$.taskId").value(TASK_ID.toString()))
			.andExpect(jsonPath("$.status").value("INCOMPLETE"))
			.andExpect(jsonPath("$.completedAt").doesNotExist())
			.andExpect(jsonPath("$.reasonSubmittedAt").value("2026-05-11T02:20:00Z"))
			.andExpect(jsonPath("$.completionNote").doesNotExist())
			.andExpect(jsonPath("$.evidenceUrl").doesNotExist())
			.andExpect(jsonPath("$.incompleteReason").value("실습을 끝내지 못했습니다."));
	}

	@Test
	void completeTaskIncompleteActionReturnsIncompleteCompletion() throws Exception {
		repository.taskExists = true;
		repository.taskReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
		repository.weeklyTask = task(TASK_ID, 1, WeeklyTaskType.ASSIGNMENT, true, TestCurriculumBeans.NOW.minusSeconds(60));
		repository.progress = progress(MemberWeekProgressStatus.IN_PROGRESS, TestCurriculumBeans.NOW.minusSeconds(3600), null, null, null, null);
		repository.nextIds = new ArrayDeque<>(List.of(COMPLETION_ID));

		mockMvc.perform(post(TASK_INCOMPLETE_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("task-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"incompleteReason":"실습을 끝내지 못했습니다."}
					"""))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(COMPLETION_ID.toString()))
			.andExpect(jsonPath("$.taskId").value(TASK_ID.toString()))
			.andExpect(jsonPath("$.status").value("INCOMPLETE"))
			.andExpect(jsonPath("$.reasonSubmittedAt").value("2026-05-11T02:20:00Z"))
			.andExpect(jsonPath("$.incompleteReason").value("실습을 끝내지 못했습니다."));
	}

	@Test
	void completeTaskIncompleteActionAllowsMissingReason() throws Exception {
		repository.taskExists = true;
		repository.taskReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
		repository.weeklyTask = task(TASK_ID, 1, WeeklyTaskType.ASSIGNMENT, true, TestCurriculumBeans.NOW.minusSeconds(60));
		repository.progress = progress(MemberWeekProgressStatus.IN_PROGRESS, TestCurriculumBeans.NOW.minusSeconds(3600), null, null, null, null);
		repository.nextIds = new ArrayDeque<>(List.of(COMPLETION_ID));

		mockMvc.perform(post(TASK_INCOMPLETE_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("task-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("INCOMPLETE"));
	}

	@Test
	void completeTaskReturnsSkippedCompletion() throws Exception {
		repository.taskExists = true;
		repository.taskReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
		repository.weeklyTask = task(TASK_ID, 1, WeeklyTaskType.CUSTOM, false, TestCurriculumBeans.NOW.plusSeconds(3600));
		repository.progress = progress(MemberWeekProgressStatus.IN_PROGRESS, TestCurriculumBeans.NOW.minusSeconds(60), null, null, null, null);
		repository.nextIds = new ArrayDeque<>(List.of(COMPLETION_ID));

		mockMvc.perform(post(TASK_COMPLETION_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("task-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"status":"SKIPPED"}
					"""))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(COMPLETION_ID.toString()))
			.andExpect(jsonPath("$.taskId").value(TASK_ID.toString()))
			.andExpect(jsonPath("$.status").value("SKIPPED"))
			.andExpect(jsonPath("$.completedAt").doesNotExist())
			.andExpect(jsonPath("$.reasonSubmittedAt").doesNotExist())
			.andExpect(jsonPath("$.completionNote").doesNotExist())
			.andExpect(jsonPath("$.incompleteReason").doesNotExist())
			.andExpect(jsonPath("$.evidenceUrl").doesNotExist());
	}

	@Test
	void completeTaskSkipActionReturnsSkippedCompletion() throws Exception {
		repository.taskExists = true;
		repository.taskReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
		repository.weeklyTask = task(TASK_ID, 1, WeeklyTaskType.CUSTOM, false, TestCurriculumBeans.NOW.plusSeconds(3600));
		repository.progress = progress(MemberWeekProgressStatus.IN_PROGRESS, TestCurriculumBeans.NOW.minusSeconds(60), null, null, null, null);
		repository.nextIds = new ArrayDeque<>(List.of(COMPLETION_ID));

		mockMvc.perform(post(TASK_SKIP_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("task-xsrf")))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(COMPLETION_ID.toString()))
			.andExpect(jsonPath("$.taskId").value(TASK_ID.toString()))
			.andExpect(jsonPath("$.status").value("SKIPPED"))
			.andExpect(jsonPath("$.completedAt").doesNotExist())
			.andExpect(jsonPath("$.reasonSubmittedAt").doesNotExist());
	}

	@Test
	void completeTaskReturnsValidationProblemWhenStatusIsMissing() throws Exception {
		mockMvc.perform(post(TASK_COMPLETION_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("task-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void completeTaskAllowsIncompleteWithoutReason() throws Exception {
		repository.taskExists = true;
		repository.taskReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
		repository.weeklyTask = task(TASK_ID, 1, WeeklyTaskType.ASSIGNMENT, true, TestCurriculumBeans.NOW.minusSeconds(60));
		repository.progress = progress(MemberWeekProgressStatus.IN_PROGRESS, TestCurriculumBeans.NOW.minusSeconds(3600), null, null, null, null);
		repository.nextIds = new ArrayDeque<>(List.of(COMPLETION_ID));

		mockMvc.perform(post(TASK_COMPLETION_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("task-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"status":"INCOMPLETE"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("INCOMPLETE"));
	}

	@Test
	void completeTaskReturnsForbiddenForPendingMember() throws Exception {
		repository.taskExists = true;
		repository.taskReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.PENDING_ONBOARDING);

		mockMvc.perform(post(TASK_COMPLETION_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("task-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"status":"DONE"}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Forbidden"));
	}

	@Test
	void completeTaskReturnsForbiddenForTaskInAnotherGroup() throws Exception {
		repository.taskExists = true;
		repository.taskReadContext = null;

		mockMvc.perform(post(TASK_COMPLETION_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("task-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"status":"DONE"}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Forbidden"));
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
			LocalDate.parse("2026-05-17"),
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
				java.util.List.of(),
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
			java.util.List.of(),
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
		return task(id, displayOrder, taskType, required, TestCurriculumBeans.NOW.plusSeconds(604800));
	}

	private static WeeklyTask task(UUID id, int displayOrder, WeeklyTaskType taskType, boolean required, Instant dueAt) {
		return new WeeklyTask(
			id,
			WEEK_ID,
			displayOrder,
			taskType,
			taskType.name() + " task",
			taskType.name() + " description",
			required,
			dueAt,
			true,
			Map.of("displayOrder", displayOrder),
			TestCurriculumBeans.NOW,
			TestCurriculumBeans.NOW
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
			MEMBER_ID,
			status,
			startedAt,
			TestCurriculumBeans.NOW.plusSeconds(604800),
			completedAt,
			completionNote,
			incompleteReason,
			reasonSubmittedAt,
			TestCurriculumBeans.NOW,
			TestCurriculumBeans.NOW
		);
	}

	private static TaskCompletion completion(
		UUID id,
		UUID taskId,
		TaskCompletionStatus status,
		Instant completedAt,
		String completionNote,
		String incompleteReason,
		Instant reasonSubmittedAt,
		String evidenceUrl
	) {
		return new TaskCompletion(
			id,
			PROGRESS_ID,
			taskId,
			MEMBER_ID,
			status,
			TestCurriculumBeans.NOW.plusSeconds(604800),
			completedAt,
			completionNote,
			incompleteReason,
			reasonSubmittedAt,
			evidenceUrl,
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
		private CurriculumStartContext taskReadContext;
		private Curriculum activeCurriculum;
		private CurriculumWeek currentWeek;
		private boolean weekExists;
		private boolean taskExists;
		private List<WeeklyTask> weeklyTasks;
		private WeeklyTask weeklyTask;
		private MemberWeekProgress progress;
		private TaskCompletion taskCompletion;
		private List<TaskCompletion> taskCompletions;
		private List<GroupActivityCount> groupActivityCounts;
		private Instant weekDueAt;
		private Queue<UUID> nextIds;

		void reset() {
			startContext = context(StudyGroupStatus.READY_TO_START, GroupMemberPermission.OWNER, GroupMemberStatus.ACTIVE);
			readContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.OWNER, GroupMemberStatus.ACTIVE);
			weekReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
			taskReadContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
			activeCurriculum = null;
			currentWeek = null;
			weekExists = false;
			taskExists = false;
			weeklyTasks = List.of();
			weeklyTask = null;
			progress = null;
			taskCompletion = null;
			taskCompletions = List.of();
			groupActivityCounts = List.of();
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
		public void saveFailedLlmUsage(LlmUsage llmUsage) {
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
		public Optional<CurriculumWeek> findWeekById(UUID weekId) {
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
		public Optional<com.studypot.aistudyleader.curriculum.domain.NextWeekTarget> findNextPendingWeek(UUID currentWeekId) {
			return Optional.empty();
		}

		@Override
		public Optional<com.studypot.aistudyleader.curriculum.domain.NextWeekTarget> findNextRegenerableWeek(UUID currentWeekId, java.time.Instant now) {
			return Optional.empty();
		}

		@Override
		public Optional<String> findLatestWeeklyReportBody(UUID groupId) {
			return Optional.empty();
		}

		@Override
		public com.studypot.aistudyleader.curriculum.domain.CurriculumWeek replaceNextWeekTasks(UUID weekId, java.util.List<com.studypot.aistudyleader.curriculum.domain.WeeklyTask> tasks, java.util.List<com.studypot.aistudyleader.curriculum.domain.RetrospectiveQuestion> retrospectiveQuestions, java.time.Instant now) {
			return null;
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
		public Optional<Instant> findCurriculumWeekStartsAt(UUID weekId) {
			return Optional.empty();
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

		@Override
		public Optional<TaskCompletion> findTaskCompletion(UUID taskId, UUID memberId) {
			return Optional.ofNullable(taskCompletion);
		}

		@Override
		public List<TaskCompletion> findTaskCompletionsByWeekIdAndMemberId(UUID weekId, UUID memberId) {
			return taskCompletions;
		}

		@Override
		public List<GroupActivityCount> findGroupDoneActivityCounts(UUID groupId, java.time.Instant fromInclusive, java.time.Instant toExclusive) {
			return groupActivityCounts;
		}

		@Override
		public int countActiveOrOnboardingMembers(UUID groupId) {
			return 2;
		}

		@Override
		public List<CurriculumWeek> findWeeksByGroupId(UUID groupId) {
			return currentWeek == null ? List.of() : List.of(currentWeek);
		}

		@Override
		public boolean insertTaskCompletion(TaskCompletion completion) {
			this.taskCompletion = completion;
			return true;
		}

		@Override
		public boolean updateTaskCompletion(TaskCompletion completion) {
			this.taskCompletion = completion;
			return true;
		}
	}
}
