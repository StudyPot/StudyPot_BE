package com.studypot.aistudyleader.studygroup.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
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
		private static final Instant NOW = Instant.parse("2026-05-09T02:30:00Z");

		@Bean
		@Primary
		StudyGroupService studyGroupService() {
			Queue<UUID> ids = new ArrayDeque<>(List.of(GROUP_ID, OWNER_MEMBER_ID));
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
	}
}
