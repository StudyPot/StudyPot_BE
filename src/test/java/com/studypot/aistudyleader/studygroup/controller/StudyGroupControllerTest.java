package com.studypot.aistudyleader.studygroup.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgressStatus;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmStructuredRequest;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberSummary;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupMemberProfile;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupJoinTarget;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import com.studypot.aistudyleader.studygroup.repository.StudyGroupRepository;
import com.studypot.aistudyleader.studygroup.service.DetailKeywordSuggestionService;
import com.studypot.aistudyleader.studygroup.service.StudyGroupService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest(
	classes = {AiStudyLeaderApplication.class, StudyGroupControllerTest.TestStudyGroupBeans.class},
	properties = {
		"studypot.cors.allowed-origins=https://studypot.netlify.app",
		"studypot.cors.allow-credentials=true"
	}
)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class StudyGroupControllerTest {

	private static final String GROUPS_PATH = ApiPaths.V1 + "/groups";
	private static final String DETAIL_KEYWORD_SUGGESTIONS_PATH = GROUPS_PATH + "/detail-keyword-suggestions";
	private static final String NETLIFY_ORIGIN = "https://studypot.netlify.app";
	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002861");
	private static final UUID OTHER_USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002865");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000002862");
	private static final UUID MISSING_GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000002866");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000002867");
	private static final String JOIN_PATH = GROUPS_PATH + "/" + GROUP_ID + "/join";
	private static final String PROFILE_PATH = GROUPS_PATH + "/" + GROUP_ID + "/members/me/profile";

	private final MockMvc mockMvc;
	private final CapturingDetailKeywordProvider detailKeywordProvider;

	@Autowired
	StudyGroupControllerTest(MockMvc mockMvc, CapturingDetailKeywordProvider detailKeywordProvider) {
		this.mockMvc = mockMvc;
		this.detailKeywordProvider = detailKeywordProvider;
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
	void suggestDetailKeywordsRequiresAuthentication() throws Exception {
		mockMvc.perform(post(DETAIL_KEYWORD_SUGGESTIONS_PATH)
				.with(xsrf("keyword-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"topic":"Spring Boot"}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void suggestDetailKeywordsRejectsInvalidRequestWithProblemDetails() throws Exception {
		mockMvc.perform(post(DETAIL_KEYWORD_SUGGESTIONS_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("keyword-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "topic": " ",
					  "hintKeywords": ["JPA"],
					  "maxCandidates": 0
					}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Invalid request payload"))
			.andExpect(jsonPath("$.fieldErrors").isArray());
	}

	@Test
	void suggestDetailKeywordsReturnsKeywordListAndDefaultsCandidateLimit() throws Exception {
		mockMvc.perform(post(DETAIL_KEYWORD_SUGGESTIONS_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("keyword-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"topic":"Spring Boot"}
					"""))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.keywords[0]").value("JPA"))
			.andExpect(jsonPath("$.keywords[1]").value("Spring Security"))
			.andExpect(jsonPath("$.suggestions").doesNotExist())
			.andExpect(jsonPath("$.rationale").doesNotExist());

		assertThat(detailKeywordProvider.request.input())
			.containsEntry("topic", "Spring Boot")
			.containsEntry("hintKeywords", List.of())
			.containsEntry("maxCandidates", 5);
	}

	@Test
	void suggestDetailKeywordsAcceptsTrustedOriginCsrfHeaderWithoutXsrfCookie() throws Exception {
		mockMvc.perform(post(DETAIL_KEYWORD_SUGGESTIONS_PATH)
				.header(HttpHeaders.ORIGIN, NETLIFY_ORIGIN)
				.header("X-XSRF-TOKEN", "bootstrapped-token")
				.with(user(USER_ID.toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"topic":"Spring Boot"}
					"""))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.keywords[0]").value("JPA"))
			.andExpect(jsonPath("$.keywords[1]").value("Spring Security"));
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
	void joinGroupByInviteCodeReturnsJoinedMember() throws Exception {
		mockMvc.perform(post(GROUPS_PATH + "/join")
				.with(user(USER_ID.toString()))
				.with(xsrf("join-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(joinRequestJson("INVITE-0001")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.groupId").value(GROUP_ID.toString()))
			.andExpect(jsonPath("$.userId").value(USER_ID.toString()))
			.andExpect(jsonPath("$.status").value("PENDING_ONBOARDING"));
	}

	@Test
	void joinGroupByInviteCodeRejectsUnknownCode() throws Exception {
		mockMvc.perform(post(GROUPS_PATH + "/join")
				.with(user(USER_ID.toString()))
				.with(xsrf("join-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(joinRequestJson("WRONG-CODE")))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
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

	@Test
	void getGroupReturnsMemberGroup() throws Exception {
		mockMvc.perform(get(GROUPS_PATH + "/" + GROUP_ID)
				.with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(GROUP_ID.toString()))
			.andExpect(jsonPath("$.name").value("Backend Interview Study"))
			.andExpect(jsonPath("$.topic").value("Spring Boot"))
			.andExpect(jsonPath("$.detailKeywords[0]").value("JPA"))
			.andExpect(jsonPath("$.status").value("ONBOARDING"))
			.andExpect(jsonPath("$.inviteCode").value("INVITE-0001"));
	}

	@Test
	void getGroupReturnsForbiddenForExistingGroupWhenUserIsNotMember() throws Exception {
		mockMvc.perform(get(GROUPS_PATH + "/" + GROUP_ID)
				.with(user(OTHER_USER_ID.toString())))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Forbidden"))
			.andExpect(jsonPath("$.detail").value("authenticated user is not a member of this study group."));
	}

	@Test
	void getGroupReturnsNotFoundForMissingGroup() throws Exception {
		mockMvc.perform(get(GROUPS_PATH + "/" + MISSING_GROUP_ID)
				.with(user(USER_ID.toString())))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Not Found"))
			.andExpect(jsonPath("$.detail").value("study group was not found."));
	}

	@Test
	void listGroupMembersRequiresAuthentication() throws Exception {
		mockMvc.perform(get(GROUPS_PATH + "/" + GROUP_ID + "/members"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void listGroupMembersReturnsMembersWithProfileFields() throws Exception {
		mockMvc.perform(get(GROUPS_PATH + "/" + GROUP_ID + "/members")
				.with(user(OTHER_USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$[0].userId").value(USER_ID.toString()))
			.andExpect(jsonPath("$[0].permission").value("OWNER"))
			.andExpect(jsonPath("$[0].status").value("ACTIVE"))
			.andExpect(jsonPath("$[0].displayName").value("현우"))
			.andExpect(jsonPath("$[0].nickname").value("hyunwoo"))
			.andExpect(jsonPath("$[0].email").value("hyunwoo@example.com"))
			.andExpect(jsonPath("$[0].onboardingStatus").value("SUBMITTED"))
			.andExpect(jsonPath("$[1].userId").value(OTHER_USER_ID.toString()))
			.andExpect(jsonPath("$[1].permission").value("MEMBER"))
			.andExpect(jsonPath("$[1].displayName").doesNotExist())
			.andExpect(jsonPath("$[1].onboardingStatus").doesNotExist());
	}

	@Test
	void listGroupMembersReturnsForbiddenForNonMember() throws Exception {
		UUID strangerUserId = UUID.fromString("018f0000-0000-7000-8000-0000000028ff");
		mockMvc.perform(get(GROUPS_PATH + "/" + GROUP_ID + "/members")
				.with(user(strangerUserId.toString())))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("authenticated user is not a member of this study group."));
	}

	@Test
	void updateGroupReturnsUpdatedGroupForOwner() throws Exception {
		mockMvc.perform(patch(GROUPS_PATH + "/" + GROUP_ID)
				.with(xsrf("update-xsrf"))
				.with(user(USER_ID.toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"새 스터디 이름\",\"topic\":\"Spring Boot\",\"detailKeywords\":[\"JPA\",\"Security\"],\"maxMembers\":6,\"startsAt\":\"2026-05-18\",\"endsAt\":\"2026-06-29\",\"description\":\"수정됨\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("새 스터디 이름"));
	}

	@Test
	void updateGroupRequiresAuthentication() throws Exception {
		mockMvc.perform(patch(GROUPS_PATH + "/" + GROUP_ID)
				.with(xsrf("update-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"이름\",\"topic\":\"Spring\",\"detailKeywords\":[\"JPA\"],\"maxMembers\":6,\"startsAt\":\"2026-05-18\",\"endsAt\":\"2026-06-29\"}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void deleteGroupReturnsNoContentForOwner() throws Exception {
		mockMvc.perform(delete(GROUPS_PATH + "/" + GROUP_ID)
				.with(xsrf("delete-xsrf"))
				.with(user(USER_ID.toString())))
			.andExpect(status().isNoContent());
	}

	@Test
	void deleteGroupRequiresAuthentication() throws Exception {
		mockMvc.perform(delete(GROUPS_PATH + "/" + GROUP_ID)
				.with(xsrf("delete-xsrf")))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void deleteGroupReturnsForbiddenForNonMember() throws Exception {
		mockMvc.perform(delete(GROUPS_PATH + "/" + GROUP_ID)
				.with(xsrf("delete-xsrf"))
				.with(user(OTHER_USER_ID.toString())))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("authenticated user is not a member of this study group."));
	}

	@Test
	void getMyGroupMemberProfileRequiresAuthentication() throws Exception {
		mockMvc.perform(get(PROFILE_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void getMyGroupMemberProfileReturnsCurrentMemberSummaries() throws Exception {
		mockMvc.perform(get(PROFILE_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.groupId").value(GROUP_ID.toString()))
			.andExpect(jsonPath("$.memberId").value(TestStudyGroupBeans.OWNER_MEMBER_ID.toString()))
			.andExpect(jsonPath("$.userId").value(USER_ID.toString()))
			.andExpect(jsonPath("$.displayName").value("현우"))
			.andExpect(jsonPath("$.permission").value("OWNER"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.onboarding.submitted").value(true))
			.andExpect(jsonPath("$.onboarding.skillLevel").value(3))
			.andExpect(jsonPath("$.onboarding.submittedAt").value("2026-05-10T01:00:00Z"))
			.andExpect(jsonPath("$.currentWeek.weekId").value(WEEK_ID.toString()))
			.andExpect(jsonPath("$.currentWeek.weekNumber").value(2))
			.andExpect(jsonPath("$.currentWeek.sprintGoal").value("JPA 실습"))
			.andExpect(jsonPath("$.currentWeek.progressStatus").value("IN_PROGRESS"))
			.andExpect(jsonPath("$.taskCompletion.totalCount").value(4))
			.andExpect(jsonPath("$.taskCompletion.doneCount").value(2))
			.andExpect(jsonPath("$.taskCompletion.incompleteCount").value(1))
			.andExpect(jsonPath("$.taskCompletion.skippedCount").value(1))
			.andExpect(jsonPath("$.retrospective.feedbackReady").value(true));
	}

	@Test
	void getMyGroupMemberProfileReturnsForbiddenForOtherUser() throws Exception {
		mockMvc.perform(get(PROFILE_PATH)
				.with(user(OTHER_USER_ID.toString())))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Forbidden"));
	}

	@Test
	void patchMyGroupMemberProfileUpdatesDisplayName() throws Exception {
		mockMvc.perform(patch(PROFILE_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("profile-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"displayName":"현우2"}
					"""))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.displayName").value("현우2"));
	}

	@Test
	void patchMyGroupMemberProfileRejectsBlankDisplayName() throws Exception {
		mockMvc.perform(patch(PROFILE_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("profile-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"displayName":" "}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Invalid request payload"));
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

		@Bean
		CapturingDetailKeywordProvider detailKeywordProvider() {
			return new CapturingDetailKeywordProvider();
		}

		@Bean
		LlmUsageRecorder llmUsageRecorder() {
			return usage -> {
			};
		}

		@Bean
		DetailKeywordSuggestionService detailKeywordSuggestionService(
			CapturingDetailKeywordProvider provider,
			LlmUsageRecorder usageRecorder
		) {
			return new DetailKeywordSuggestionService(
				provider,
				JsonMapper.builder().findAndAddModules().build(),
				usageRecorder,
				Clock.fixed(NOW, ZoneOffset.UTC),
				() -> UUID.fromString("018f0000-0000-7000-8000-000000002865")
			);
		}
	}

	static final class CapturingDetailKeywordProvider implements LlmProviderClient {

		private LlmStructuredRequest request;

		@Override
		public LlmStructuredResponse requestStructured(LlmStructuredRequest request) {
			this.request = request;
			return new LlmStructuredResponse(
				LlmProvider.OPENAI,
				"gpt-4o-mini",
				"""
					{"keywords":["JPA","Spring Security"]}
					""",
				20,
				12,
				BigDecimal.ZERO,
				80,
				LlmUsageStatus.SUCCESS,
				null,
				Map.of("purpose", "DETAIL_KEYWORD_SUGGEST"),
				"raw provider response"
			);
		}
	}

	private static final class NoopStudyGroupRepository implements StudyGroupRepository {

		private String profileDisplayName = "현우";

		@Override
		public void saveCreatedGroup(StudyGroup group, GroupMember ownerMember) {
		}

		@Override
		public java.util.Optional<StudyGroupJoinTarget> findJoinTargetByIdForUpdate(UUID groupId) {
			return java.util.Optional.of(new StudyGroupJoinTarget(groupId, StudyGroupStatus.ACTIVE, 6, "INVITE-0001"));
		}

		@Override
		public java.util.Optional<StudyGroupJoinTarget> findJoinTargetByInviteCode(String inviteCode) {
			if (!"INVITE-0001".equals(inviteCode)) {
				return java.util.Optional.empty();
			}
			return java.util.Optional.of(new StudyGroupJoinTarget(GROUP_ID, StudyGroupStatus.ACTIVE, 6, "INVITE-0001"));
		}

		@Override
		public boolean existsActiveOrOnboardingMember(UUID groupId, UUID userId) {
			return GROUP_ID.equals(groupId) && OTHER_USER_ID.equals(userId);
		}

		@Override
		public List<GroupMemberSummary> findGroupMembers(UUID groupId) {
			if (!GROUP_ID.equals(groupId)) {
				return List.of();
			}
			return List.of(
				new GroupMemberSummary(
					TestStudyGroupBeans.OWNER_MEMBER_ID, GROUP_ID, USER_ID,
					GroupMemberPermission.OWNER, GroupMemberStatus.ACTIVE,
					"현우", "hyunwoo", "hyunwoo@example.com", "SUBMITTED"
				),
				new GroupMemberSummary(
					TestStudyGroupBeans.JOINED_MEMBER_ID, GROUP_ID, OTHER_USER_ID,
					GroupMemberPermission.MEMBER, GroupMemberStatus.PENDING_ONBOARDING,
					null, "minsu", "minsu@example.com", null
				)
			);
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
			return List.of(testGroup());
		}

		@Override
		public java.util.Optional<StudyGroupMemberProfile> findMyGroupMemberProfile(UUID groupId, UUID userId) {
			if (!GROUP_ID.equals(groupId) || !USER_ID.equals(userId)) {
				return java.util.Optional.empty();
			}
			return java.util.Optional.of(profile(profileDisplayName));
		}

		@Override
		public boolean updateMyGroupMemberDisplayName(UUID groupId, UUID userId, String displayName, Instant updatedAt) {
			this.profileDisplayName = displayName;
			return GROUP_ID.equals(groupId) && USER_ID.equals(userId);
		}

		@Override
		public boolean softDeleteGroup(UUID groupId, Instant deletedAt) {
			return GROUP_ID.equals(groupId);
		}

		@Override
		public boolean updateGroup(StudyGroup group) {
			return GROUP_ID.equals(group.id());
		}

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			return GROUP_ID.equals(groupId);
		}

		@Override
		public java.util.Optional<StudyGroup> findGroupByIdForMemberUserId(UUID groupId, UUID userId) {
			if (!GROUP_ID.equals(groupId) || !USER_ID.equals(userId)) {
				return java.util.Optional.empty();
			}
			return java.util.Optional.of(testGroup());
		}

		private static StudyGroup testGroup() {
			return StudyGroup.create(
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
			);
		}

		private static StudyGroupMemberProfile profile(String displayName) {
			return new StudyGroupMemberProfile(
				GROUP_ID,
				TestStudyGroupBeans.OWNER_MEMBER_ID,
				USER_ID,
				displayName,
				GroupMemberPermission.OWNER,
				GroupMemberStatus.ACTIVE,
				new StudyGroupMemberProfile.OnboardingSummary(
					true,
					3,
					Instant.parse("2026-05-10T01:00:00Z")
				),
				new StudyGroupMemberProfile.CurrentWeekSummary(
					WEEK_ID,
					2,
					"JPA 실습",
					Instant.parse("2026-05-17T00:00:00Z"),
					Instant.parse("2026-05-24T00:00:00Z"),
					MemberWeekProgressStatus.IN_PROGRESS
				),
				new StudyGroupMemberProfile.TaskCompletionSummary(4, 2, 1, 1),
				new StudyGroupMemberProfile.RetrospectiveSummary(true)
			);
		}
	}
}
