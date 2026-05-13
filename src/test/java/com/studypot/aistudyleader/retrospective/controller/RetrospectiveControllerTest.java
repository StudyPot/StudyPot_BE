package com.studypot.aistudyleader.retrospective.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgressStatus;
import com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.retrospective.domain.Retrospective;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveAiContext;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveMembershipContext;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveProgress;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveStatus;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveTaskSummary;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveTriggerType;
import com.studypot.aistudyleader.retrospective.repository.RetrospectiveRepository;
import com.studypot.aistudyleader.retrospective.service.RetrospectiveService;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(classes = {AiStudyLeaderApplication.class, RetrospectiveControllerTest.TestRetrospectiveBeans.class})
@AutoConfigureMockMvc
class RetrospectiveControllerTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000006201");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000006202");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000006203");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000006204");
	private static final UUID PROGRESS_ID = UUID.fromString("018f0000-0000-7000-8000-000000006205");
	private static final UUID RETROSPECTIVE_ID = UUID.fromString("018f0000-0000-7000-8000-000000006206");
	private static final UUID TASK_ID = UUID.fromString("018f0000-0000-7000-8000-000000006207");
	private static final String RETROSPECTIVE_PATH = ApiPaths.V1 + "/weeks/" + WEEK_ID + "/retrospectives/me";

	private final MockMvc mockMvc;
	private final MutableRetrospectiveRepository repository;

	@Autowired
	RetrospectiveControllerTest(MockMvc mockMvc, MutableRetrospectiveRepository repository) {
		this.mockMvc = mockMvc;
		this.repository = repository;
	}

	@BeforeEach
	void setUp() {
		repository.reset();
	}

	@Test
	void requestRetrospectiveRequiresAuthentication() throws Exception {
		mockMvc.perform(post(RETROSPECTIVE_PATH)
				.with(xsrf("retro-xsrf")))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void requestRetrospectiveReturnsAcceptedPendingRetrospective() throws Exception {
		mockMvc.perform(post(RETROSPECTIVE_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("retro-xsrf")))
			.andExpect(status().isAccepted())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(RETROSPECTIVE_ID.toString()))
			.andExpect(jsonPath("$.status").value("PENDING"))
			.andExpect(jsonPath("$.aiFeedback").isMap())
			.andExpect(jsonPath("$.nextWeekAdjustment").isMap());
	}

	@Test
	void requestRetrospectiveReturnsForbiddenForPendingMember() throws Exception {
		repository.membership = new RetrospectiveMembershipContext(
			GROUP_ID,
			MEMBER_ID,
			StudyGroupStatus.ACTIVE,
			GroupMemberPermission.MEMBER,
			GroupMemberStatus.PENDING_ONBOARDING
		);

		mockMvc.perform(post(RETROSPECTIVE_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("retro-xsrf")))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Forbidden"));
	}

	@Test
	void getMyRetrospectiveReturnsExistingRetrospective() throws Exception {
		repository.existingRetrospective = completedRetrospective();

		mockMvc.perform(get(RETROSPECTIVE_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(RETROSPECTIVE_ID.toString()))
			.andExpect(jsonPath("$.status").value("COMPLETED"))
			.andExpect(jsonPath("$.aiFeedback.summary").value("이번 주 학습 흐름이 좋습니다."))
			.andExpect(jsonPath("$.nextWeekAdjustment.focus").value("JPA 심화"));
	}

	@Test
	void getMyRetrospectiveReturnsNotFoundWhenMissing() throws Exception {
		mockMvc.perform(get(RETROSPECTIVE_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isNotFound())
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

	private static RetrospectiveProgress progress() {
		return new RetrospectiveProgress(
			PROGRESS_ID,
			WEEK_ID,
			MEMBER_ID,
			MemberWeekProgressStatus.COMPLETED,
			TestRetrospectiveBeans.NOW.minusSeconds(3600),
			TestRetrospectiveBeans.WEEK_DUE_AT,
			TestRetrospectiveBeans.NOW.minusSeconds(600),
			"완료했습니다.",
			null,
			null
		);
	}

	private static RetrospectiveTaskSummary taskSummary() {
		return new RetrospectiveTaskSummary(
			TASK_ID,
			1,
			WeeklyTaskType.READING,
			"JPA 읽기",
			true,
			TestRetrospectiveBeans.WEEK_DUE_AT,
			TaskCompletionStatus.DONE,
			TestRetrospectiveBeans.NOW.minusSeconds(900),
			"정리 완료",
			null,
			null
		);
	}

	private static Retrospective completedRetrospective() {
		return new Retrospective(
			RETROSPECTIVE_ID,
			PROGRESS_ID,
			WEEK_ID,
			MEMBER_ID,
			null,
			RetrospectiveTriggerType.MANUAL,
			Map.of("progress", Map.of("status", "COMPLETED")),
			Map.of("summary", "이번 주 학습 흐름이 좋습니다."),
			Map.of("focus", "JPA 심화"),
			RetrospectiveStatus.COMPLETED,
			TestRetrospectiveBeans.NOW.minusSeconds(120),
			TestRetrospectiveBeans.NOW,
			TestRetrospectiveBeans.NOW.minusSeconds(120),
			TestRetrospectiveBeans.NOW
		);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestRetrospectiveBeans {

		private static final Instant NOW = Instant.parse("2026-05-12T03:00:00Z");
		private static final Instant WEEK_DUE_AT = Instant.parse("2026-05-18T03:00:00Z");

		@Bean
		@Primary
		MutableRetrospectiveRepository mutableRetrospectiveRepository() {
			return new MutableRetrospectiveRepository();
		}

		@Bean
		@Primary
		RetrospectiveService testRetrospectiveService(MutableRetrospectiveRepository repository) {
			return new RetrospectiveService(repository, Clock.fixed(NOW, ZoneOffset.UTC), repository::nextId);
		}
	}

	static final class MutableRetrospectiveRepository implements RetrospectiveRepository {

		private boolean weekExists;
		private RetrospectiveMembershipContext membership;
		private RetrospectiveProgress progress;
		private Retrospective existingRetrospective;
		private List<RetrospectiveTaskSummary> taskSummaries;
		private Deque<UUID> ids;

		void reset() {
			weekExists = true;
			membership = new RetrospectiveMembershipContext(
				GROUP_ID,
				MEMBER_ID,
				StudyGroupStatus.ACTIVE,
				GroupMemberPermission.MEMBER,
				GroupMemberStatus.ACTIVE
			);
			progress = progress();
			existingRetrospective = null;
			taskSummaries = List.of(taskSummary());
			ids = new ArrayDeque<>(List.of(RETROSPECTIVE_ID));
		}

		UUID nextId() {
			UUID id = ids.poll();
			if (id == null) {
				throw new IllegalStateException("no deterministic id left");
			}
			return id;
		}

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
		public RetrospectiveAiContext findAiContext(UUID groupId, UUID memberId, UUID weekId, UUID retrospectiveId) {
			return RetrospectiveAiContext.empty();
		}

		@Override
		public boolean insertRetrospective(Retrospective retrospective) {
			existingRetrospective = retrospective;
			return true;
		}

		@Override
		public Optional<Retrospective> findRetrospectiveById(UUID retrospectiveId) {
			if (retrospectiveId == null || existingRetrospective == null) {
				return Optional.empty();
			}
			if (!retrospectiveId.equals(existingRetrospective.id())) {
				return Optional.empty();
			}
			return Optional.of(existingRetrospective);
		}

		@Override
		public boolean updateRetrospectiveResult(Retrospective retrospective) {
			existingRetrospective = retrospective;
			return true;
		}
	}
}
