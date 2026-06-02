package com.studypot.aistudyleader.onboarding.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.onboarding.domain.GroupOnboardingResponse;
import com.studypot.aistudyleader.onboarding.domain.MemberAvailabilitySlot;
import com.studypot.aistudyleader.onboarding.domain.OnboardingMemberContext;
import com.studypot.aistudyleader.onboarding.repository.OnboardingRepository;
import com.studypot.aistudyleader.onboarding.service.OnboardingService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(classes = {AiStudyLeaderApplication.class, OnboardingControllerTest.TestOnboardingBeans.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OnboardingControllerTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000003061");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000003062");
	private static final String ONBOARDING_PATH = ApiPaths.V1 + "/groups/" + GROUP_ID + "/onboarding/me";

	private final MockMvc mockMvc;

	@Autowired
	OnboardingControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void submitMyOnboardingRequiresAuthentication() throws Exception {
		mockMvc.perform(post(ONBOARDING_PATH)
				.with(xsrf("onboarding-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequestJson()))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void submitMyOnboardingRejectsInvalidSkillLevel() throws Exception {
		mockMvc.perform(post(ONBOARDING_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("onboarding-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "skillLevel": 0,
					  "availabilitySlots": [
					    {"dayOfWeek": 1, "startTime": "20:00", "endTime": "21:00", "timezone": "Asia/Seoul"}
					  ]
					}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Invalid request payload"))
			.andExpect(jsonPath("$.fieldErrors").isArray());
	}

	@Test
	void submitMyOnboardingRejectsInvalidAvailabilityTimeWindow() throws Exception {
		mockMvc.perform(post(ONBOARDING_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("onboarding-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "skillLevel": 3,
					  "availabilitySlots": [
					    {"dayOfWeek": 1, "startTime": "22:00", "endTime": "20:00", "timezone": "Asia/Seoul"}
					  ]
					}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Invalid request payload"))
			.andExpect(jsonPath("$.fieldErrors").isArray());
	}

	@Test
	void submitMyOnboardingRejectsMalformedAvailabilityTime() throws Exception {
		mockMvc.perform(post(ONBOARDING_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("onboarding-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "skillLevel": 3,
					  "availabilitySlots": [
					    {"dayOfWeek": 1, "startTime": "8pm", "endTime": "21:00", "timezone": "Asia/Seoul"}
					  ]
					}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Invalid request payload"))
			.andExpect(jsonPath("$.fieldErrors").isArray());
	}

	@Test
	void submitMyOnboardingRejectsInvalidAvailabilityTimezone() throws Exception {
		mockMvc.perform(post(ONBOARDING_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("onboarding-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "skillLevel": 3,
					  "availabilitySlots": [
					    {"dayOfWeek": 1, "startTime": "20:00", "endTime": "21:00", "timezone": "Mars/Base"}
					  ]
					}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Invalid request payload"))
			.andExpect(jsonPath("$.fieldErrors").isArray());
	}

	@Test
	void submitMyOnboardingReturnsSubmittedResponseWithoutInternalMaps() throws Exception {
		mockMvc.perform(post(ONBOARDING_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("onboarding-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequestJson()))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.groupId").value(GROUP_ID.toString()))
			.andExpect(jsonPath("$.memberId").value(TestOnboardingBeans.MEMBER_ID.toString()))
			.andExpect(jsonPath("$.skillLevel").value(3))
			.andExpect(jsonPath("$.keywordSkillLevels").doesNotExist())
			.andExpect(jsonPath("$.taskPreferences").doesNotExist())
			.andExpect(jsonPath("$.additionalNote").value("JPA는 처음이고 실습 위주가 좋아요."))
			.andExpect(jsonPath("$.availabilitySlots").isArray())
			.andExpect(jsonPath("$.availabilitySlots[0].dayOfWeek").value(2))
			.andExpect(jsonPath("$.availabilitySlots[0].startTime").value("20:00"))
			.andExpect(jsonPath("$.availabilitySlots[0].endTime").value("22:00"))
			.andExpect(jsonPath("$.availabilitySlots[0].timezone").value("Asia/Seoul"))
			.andExpect(jsonPath("$.status").value("SUBMITTED"))
			.andExpect(jsonPath("$.submittedAt").value("2026-05-09T08:30:00Z"));
	}

	@Test
	void getMyOnboardingReturnsSimplifiedResponse() throws Exception {
		mockMvc.perform(get(ONBOARDING_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("onboarding-xsrf")))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.groupId").value(GROUP_ID.toString()))
			.andExpect(jsonPath("$.skillLevel").value(2))
			.andExpect(jsonPath("$.keywordSkillLevels").doesNotExist())
			.andExpect(jsonPath("$.taskPreferences").doesNotExist())
			.andExpect(jsonPath("$.availabilitySlots[0].dayOfWeek").value(2))
			.andExpect(jsonPath("$.status").value("DRAFT"));
	}

	private static String validRequestJson() {
		return """
			{
			  "skillLevel": 3,
			  "additionalNote": "JPA는 처음이고 실습 위주가 좋아요.",
			  "availabilitySlots": [
			    {"dayOfWeek": 2, "startTime": "20:00", "endTime": "22:00", "timezone": "Asia/Seoul"}
			  ]
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
	static class TestOnboardingBeans {

		private static final UUID RESPONSE_ID = UUID.fromString("018f0000-0000-7000-8000-000000003063");
		private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000003064");
		private static final UUID SLOT_ID = UUID.fromString("018f0000-0000-7000-8000-000000003065");
		private static final Instant NOW = Instant.parse("2026-05-09T08:30:00Z");
		private static final OnboardingMemberContext CONTEXT = new OnboardingMemberContext(
			GROUP_ID,
			MEMBER_ID,
			List.of("JPA", "Security")
		);

		@Bean
		@Primary
		OnboardingService onboardingService() {
			return new OnboardingService(new InMemoryOnboardingRepository(), Clock.fixed(NOW, ZoneOffset.UTC), new DeterministicIds());
		}

		private static final class InMemoryOnboardingRepository implements OnboardingRepository {

			private GroupOnboardingResponse response = GroupOnboardingResponse.draft(
				RESPONSE_ID,
				CONTEXT,
				Map.of("JPA", 2, "Security", 2),
				Map.of(),
				"실습 위주가 좋아요.",
				NOW
			).withAvailabilitySlots(List.of(slot()));

			@Override
			public boolean existsStudyGroup(UUID groupId) {
				return true;
			}

			@Override
			public Optional<OnboardingMemberContext> findMemberContext(UUID groupId, UUID userId) {
				return Optional.of(CONTEXT);
			}

			@Override
			public Optional<GroupOnboardingResponse> findResponseByMemberId(UUID memberId) {
				return Optional.ofNullable(response);
			}

			@Override
			public GroupOnboardingResponse saveDraft(GroupOnboardingResponse response) {
				this.response = response;
				return response;
			}

			@Override
			public GroupOnboardingResponse submit(GroupOnboardingResponse response) {
				this.response = response;
				return response;
			}

			@Override
			public boolean activatePendingMember(UUID memberId, Instant activatedAt) {
				return true;
			}

			@Override
			public boolean markStudyGroupReadyToStartIfOwnerOnboardingComplete(UUID groupId, UUID memberId, Instant readyAt) {
				return true;
			}

			private static MemberAvailabilitySlot slot() {
				return MemberAvailabilitySlot.create(
					SLOT_ID,
					RESPONSE_ID,
					MEMBER_ID,
					2,
					"20:00",
					"22:00",
					"Asia/Seoul",
					NOW
				);
			}
		}

		private static final class DeterministicIds implements java.util.function.Supplier<UUID> {

			private final List<UUID> ids = List.of(SLOT_ID);
			private int index;

			@Override
			public UUID get() {
				if (index >= ids.size()) {
					throw new AssertionError("no deterministic id left");
				}
				UUID next = ids.get(index);
				index++;
				return next;
			}
		}
	}
}
