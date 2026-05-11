package com.studypot.aistudyleader.studygroup.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupJoinTarget;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import com.studypot.aistudyleader.studygroup.repository.StudyGroupRepository;
import com.studypot.aistudyleader.studygroup.service.StudyGroupService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest(classes = {AiStudyLeaderApplication.class, StudyGroupControllerTest.TestStudyGroupBeans.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class StudyGroupControllerTest {

	private static final String GROUPS_PATH = ApiPaths.V1 + "/groups";
	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002861");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000002862");
	private static final String JOIN_PATH = GROUPS_PATH + "/" + GROUP_ID + "/join";

	private final MockMvc mockMvc;

	@Autowired
	StudyGroupControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void createGroupRequiresAuthentication() throws Exception {
		mockMvc.perform(post(GROUPS_PATH)
				.with(xsrf("create-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequestJson()))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void createGroupRejectsInvalidRequestWithProblemDetails() throws Exception {
		mockMvc.perform(post(GROUPS_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("create-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "",
					  "topic": "Spring Boot",
					  "detailKeywords": [],
					  "maxMembers": 0,
					  "startsAt": "2026-06-21",
					  "endsAt": "2026-05-10"
					}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Invalid request payload"))
			.andExpect(jsonPath("$.fieldErrors").isArray());
	}

	@Test
	void createGroupReturnsCreatedStudyGroupResponse() throws Exception {
		mockMvc.perform(post(GROUPS_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("create-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequestJson()))
			.andExpect(status().isCreated())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(GROUP_ID.toString()))
			.andExpect(jsonPath("$.name").value("Backend Interview Study"))
			.andExpect(jsonPath("$.topic").value("Spring Boot"))
			.andExpect(jsonPath("$.detailKeywords[0]").value("JPA"))
			.andExpect(jsonPath("$.detailKeywords[1]").value("Security"))
			.andExpect(jsonPath("$.status").value("ONBOARDING"))
			.andExpect(jsonPath("$.maxMembers").value(6))
			.andExpect(jsonPath("$.inviteCode").value("INVITE-0001"))
			.andExpect(jsonPath("$.startsAt").value("2026-05-10"))
			.andExpect(jsonPath("$.endsAt").value("2026-06-21"));
	}

	@Test
	void joinGroupRequiresAuthentication() throws Exception {
		mockMvc.perform(post(JOIN_PATH)
				.with(xsrf("join-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(joinRequestJson("INVITE-0001")))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void joinGroupRejectsInvalidRequestWithProblemDetails() throws Exception {
		mockMvc.perform(post(JOIN_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("join-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(joinRequestJson(" ")))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Invalid request payload"))
			.andExpect(jsonPath("$.fieldErrors").isArray());
	}

	@Test
	void joinGroupReturnsJoinedMemberResponse() throws Exception {
		mockMvc.perform(post(JOIN_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("join-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(joinRequestJson("INVITE-0001")))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.groupId").value(GROUP_ID.toString()))
			.andExpect(jsonPath("$.userId").value(USER_ID.toString()))
			.andExpect(jsonPath("$.permission").value("MEMBER"))
			.andExpect(jsonPath("$.status").value("PENDING_ONBOARDING"));
	}

	@Test
	void joinGroupReturnsConflictProblemForMismatchedInviteCode() throws Exception {
		mockMvc.perform(post(JOIN_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("join-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(joinRequestJson("WRONG-CODE")))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Conflict"))
			.andExpect(jsonPath("$.detail").value("invite code does not match the study group."));
	}

	@Test
	void listGroupsRequiresAuthentication() throws Exception {
		mockMvc.perform(get(GROUPS_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void listGroupsReturnsMyGroups() throws Exception {
		mockMvc.perform(get(GROUPS_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$[0].id").value(GROUP_ID.toString()))
			.andExpect(jsonPath("$[0].name").value("Backend Interview Study"))
			.andExpect(jsonPath("$[0].topic").value("Spring Boot"))
			.andExpect(jsonPath("$[0].detailKeywords[0]").value("JPA"))
			.andExpect(jsonPath("$[0].status").value("ONBOARDING"))
			.andExpect(jsonPath("$[0].maxMembers").value(6))
			.andExpect(jsonPath("$[0].inviteCode").value("INVITE-0001"));
	}

	private static String validRequestJson() {
		return """
			{
			  "name": "Backend Interview Study",
			  "topic": "Spring Boot",
			  "detailKeywords": ["JPA", "Security"],
			  "maxMembers": 6,
			  "startsAt": "2026-05-10",
			  "endsAt": "2026-06-21",
			  "description": "Weekly backend interview prep"
			}
			""";
	}

	private static String joinRequestJson(String inviteCode) {
		return """
			{
			  "inviteCode": "%s"
			}
			""".formatted(inviteCode);
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

	@TestConfiguration(proxyBeanMethods = false)
	static class TestStudyGroupBeans {

		private static final UUID OWNER_MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002863");
		private static final UUID JOINED_MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002864");
		private static final Instant NOW = Instant.parse("2026-05-09T02:30:00Z");

		@Bean
		@Primary
		StudyGroupService studyGroupService() {
			Queue<UUID> ids = new ArrayDeque<>(List.of(GROUP_ID, OWNER_MEMBER_ID, JOINED_MEMBER_ID));
			Supplier<UUID> idGenerator = () -> {
				UUID id = ids.poll();
				if (id == null) {
					throw new IllegalStateException("No test UUID left.");
				}
				return id;
			};
			return new StudyGroupService(
				new NoopStudyGroupRepository(),
				Clock.fixed(NOW, ZoneOffset.UTC),
				idGenerator,
				() -> "INVITE-0001",
				3
			);
		}
	}

	private static final class NoopStudyGroupRepository implements StudyGroupRepository {

		@Override
		public void saveCreatedGroup(StudyGroup group, GroupMember ownerMember) {
		}

		@Override
		public java.util.Optional<StudyGroupJoinTarget> findJoinTargetByIdForUpdate(UUID groupId) {
			return java.util.Optional.of(new StudyGroupJoinTarget(groupId, StudyGroupStatus.ONBOARDING, 6, "INVITE-0001"));
		}

		@Override
		public boolean existsActiveOrOnboardingMember(UUID groupId, UUID userId) {
			return false;
		}

		@Override
		public int countActiveOrOnboardingMembers(UUID groupId) {
			return 1;
		}

		@Override
		public void saveJoinedMember(GroupMember member) {
		}

		@Override
		public List<StudyGroup> findGroupsByMemberUserId(UUID userId) {
			return List.of(StudyGroup.create(
				GROUP_ID,
				USER_ID,
				"Backend Interview Study",
				"Spring Boot",
				List.of("JPA", "Security"),
				6,
				java.time.LocalDate.parse("2026-05-10"),
				java.time.LocalDate.parse("2026-06-21"),
				"Weekly backend interview prep",
				"INVITE-0001",
				TestStudyGroupBeans.NOW
			));
		}
	}
}
