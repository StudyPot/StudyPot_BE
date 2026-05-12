package com.studypot.aistudyleader.curriculum.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.curriculum.domain.Curriculum;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGeneration;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStartContext;
import com.studypot.aistudyleader.curriculum.domain.CurriculumTaskPlan;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeekPlan;
import com.studypot.aistudyleader.curriculum.domain.LlmProvider;
import com.studypot.aistudyleader.curriculum.domain.LlmUsage;
import com.studypot.aistudyleader.curriculum.domain.LlmUsageStatus;
import com.studypot.aistudyleader.curriculum.domain.SubmittedOnboardingResponse;
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
	private static final String START_PATH = ApiPaths.V1 + "/groups/" + GROUP_ID + "/start";
	private static final String CURRICULUM_PATH = ApiPaths.V1 + "/groups/" + GROUP_ID + "/curriculum";

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
			Queue<UUID> ids = new ArrayDeque<>(List.of(LLM_USAGE_ID, CURRICULUM_ID, WEEK_ID, TASK_ID));
			return new CurriculumService(
				repository,
				request -> generation(),
				Clock.fixed(NOW, ZoneOffset.UTC),
				() -> {
					UUID id = ids.poll();
					if (id == null) {
						throw new IllegalStateException("No test UUID left.");
					}
					return id;
				}
			);
		}
	}

	static final class MutableCurriculumRepository implements CurriculumRepository {

		private CurriculumStartContext startContext;
		private CurriculumStartContext readContext;
		private Curriculum activeCurriculum;

		void reset() {
			startContext = context(StudyGroupStatus.ONBOARDING, GroupMemberPermission.OWNER, GroupMemberStatus.ACTIVE);
			readContext = context(StudyGroupStatus.ACTIVE, GroupMemberPermission.OWNER, GroupMemberStatus.ACTIVE);
			activeCurriculum = null;
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
	}
}
