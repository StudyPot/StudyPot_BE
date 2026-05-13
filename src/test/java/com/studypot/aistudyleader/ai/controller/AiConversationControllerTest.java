package com.studypot.aistudyleader.ai.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.ai.domain.AiConversation;
import com.studypot.aistudyleader.ai.domain.AiConversationMembershipContext;
import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageContext;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageCursor;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageSenderType;
import com.studypot.aistudyleader.ai.domain.AiConversationPromptContext;
import com.studypot.aistudyleader.ai.domain.AiConversationStatus;
import com.studypot.aistudyleader.ai.domain.AiConversationType;
import com.studypot.aistudyleader.ai.domain.AiRetrospectiveReference;
import com.studypot.aistudyleader.ai.repository.AiConversationRepository;
import com.studypot.aistudyleader.ai.service.AiConversationService;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
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

@SpringBootTest(classes = {AiStudyLeaderApplication.class, AiConversationControllerTest.TestAiConversationBeans.class})
@AutoConfigureMockMvc
class AiConversationControllerTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000009301");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000009302");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000009303");
	private static final UUID OTHER_MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000009308");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000009304");
	private static final UUID RETROSPECTIVE_ID = UUID.fromString("018f0000-0000-7000-8000-000000009305");
	private static final UUID CONVERSATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000009306");
	private static final UUID MESSAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000009307");
	private static final String CONVERSATION_PATH = ApiPaths.V1 + "/groups/" + GROUP_ID + "/ai-conversations";
	private static final String MESSAGE_PATH = ApiPaths.V1 + "/ai-conversations/" + CONVERSATION_ID + "/messages";

	private final MockMvc mockMvc;
	private final MutableAiConversationRepository repository;

	@Autowired
	AiConversationControllerTest(MockMvc mockMvc, MutableAiConversationRepository repository) {
		this.mockMvc = mockMvc;
		this.repository = repository;
	}

	@BeforeEach
	void setUp() {
		repository.reset();
	}

	@Test
	void openConversationRequiresAuthentication() throws Exception {
		mockMvc.perform(post(CONVERSATION_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"conversationType":"TEAM_LEAD_CHAT"}
					""")
				.with(xsrf("ai-xsrf")))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void openConversationReturnsCreatedSession() throws Exception {
		mockMvc.perform(post(CONVERSATION_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("ai-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"conversationType":"TEAM_LEAD_CHAT"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(CONVERSATION_ID.toString()))
			.andExpect(jsonPath("$.conversationType").value("TEAM_LEAD_CHAT"))
			.andExpect(jsonPath("$.status").value("OPEN"))
			.andExpect(jsonPath("$.summary").value(""));
	}

	@Test
	void openRetrospectiveConversationLinksRetrospective() throws Exception {
		repository.retrospectiveReference = new AiRetrospectiveReference(GROUP_ID, MEMBER_ID, WEEK_ID);

		mockMvc.perform(post(CONVERSATION_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("ai-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"conversationType":"RETROSPECTIVE","retrospectiveId":"%s"}
					""".formatted(RETROSPECTIVE_ID)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(CONVERSATION_ID.toString()))
			.andExpect(jsonPath("$.conversationType").value("RETROSPECTIVE"))
			.andExpect(jsonPath("$.status").value("OPEN"));
	}

	@Test
	void openConversationReturnsValidationProblemWhenTypeMissing() throws Exception {
		mockMvc.perform(post(CONVERSATION_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("ai-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void openConversationReturnsForbiddenForPendingMember() throws Exception {
		repository.membership = new AiConversationMembershipContext(
			GROUP_ID,
			MEMBER_ID,
			StudyGroupStatus.ACTIVE,
			GroupMemberPermission.MEMBER,
			GroupMemberStatus.PENDING_ONBOARDING
		);

		mockMvc.perform(post(CONVERSATION_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("ai-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"conversationType":"TEAM_LEAD_CHAT"}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Forbidden"));
	}

	@Test
	void openConversationReturnsForbiddenForLeftMember() throws Exception {
		repository.membership = new AiConversationMembershipContext(
			GROUP_ID,
			MEMBER_ID,
			StudyGroupStatus.ACTIVE,
			GroupMemberPermission.MEMBER,
			GroupMemberStatus.LEFT
		);

		mockMvc.perform(post(CONVERSATION_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("ai-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"conversationType":"TEAM_LEAD_CHAT"}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Forbidden"));
	}

	@Test
	void openConversationReturnsForbiddenForCrossMemberRetrospective() throws Exception {
		repository.retrospectiveReference = new AiRetrospectiveReference(GROUP_ID, OTHER_MEMBER_ID, WEEK_ID);

		mockMvc.perform(post(CONVERSATION_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("ai-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"conversationType":"RETROSPECTIVE","retrospectiveId":"%s"}
					""".formatted(RETROSPECTIVE_ID)))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Forbidden"));
	}

	@Test
	void sendMessageReturnsCreatedUserMessage() throws Exception {
		repository.nextIds(MESSAGE_ID);

		mockMvc.perform(post(MESSAGE_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("ai-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"content":"  이번 주 과제 양을 줄이고 싶어요.  "}
					"""))
			.andExpect(status().isCreated())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(MESSAGE_ID.toString()))
			.andExpect(jsonPath("$.senderType").value("USER"))
			.andExpect(jsonPath("$.content").value("이번 주 과제 양을 줄이고 싶어요."))
			.andExpect(jsonPath("$.createdAt").value("2026-05-13T01:15:00Z"));

		assertThat(repository.insertedMessage.senderType()).isEqualTo(AiConversationMessageSenderType.USER);
	}

	@Test
	void sendMessageReturnsValidationProblemWhenContentBlank() throws Exception {
		mockMvc.perform(post(MESSAGE_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("ai-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"content":" "}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void sendMessageReturnsConflictWhenConversationClosed() throws Exception {
		repository.nextIds(MESSAGE_ID);
		repository.messageContext = new AiConversationMessageContext(
			CONVERSATION_ID,
			GROUP_ID,
			MEMBER_ID,
			AiConversationStatus.CLOSED,
			StudyGroupStatus.ACTIVE,
			GroupMemberStatus.ACTIVE
		);

		mockMvc.perform(post(MESSAGE_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("ai-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"content":"닫힌 대화에는 저장할 수 없습니다."}
					"""))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void sendMessageReturnsForbiddenForOtherUserConversation() throws Exception {
		repository.nextIds(MESSAGE_ID);
		repository.messageContext = null;

		mockMvc.perform(post(MESSAGE_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("ai-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"content":"다른 멤버 대화에 접근할 수 없습니다."}
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

	@TestConfiguration(proxyBeanMethods = false)
	static class TestAiConversationBeans {

		private static final Instant NOW = Instant.parse("2026-05-13T01:15:00Z");

		@Bean
		@Primary
		MutableAiConversationRepository mutableAiConversationRepository() {
			return new MutableAiConversationRepository();
		}

		@Bean
		@Primary
		AiConversationService testAiConversationService(MutableAiConversationRepository repository) {
			return new AiConversationService(repository, Clock.fixed(NOW, ZoneOffset.UTC), repository::nextId);
		}
	}

	static final class MutableAiConversationRepository implements AiConversationRepository {

		private boolean groupExists;
		private AiConversationMembershipContext membership;
		private UUID weekGroupId;
		private AiRetrospectiveReference retrospectiveReference;
		private AiConversationMessageContext messageContext;
		private Deque<UUID> ids;
		private AiConversation insertedConversation;
		private AiConversationMessage insertedMessage;

		void reset() {
			groupExists = true;
			membership = new AiConversationMembershipContext(
				GROUP_ID,
				MEMBER_ID,
				StudyGroupStatus.ACTIVE,
				GroupMemberPermission.MEMBER,
				GroupMemberStatus.ACTIVE
			);
			weekGroupId = GROUP_ID;
			retrospectiveReference = null;
			messageContext = new AiConversationMessageContext(
				CONVERSATION_ID,
				GROUP_ID,
				MEMBER_ID,
				AiConversationStatus.OPEN,
				StudyGroupStatus.ACTIVE,
				GroupMemberStatus.ACTIVE
			);
			nextIds(CONVERSATION_ID);
			insertedConversation = null;
			insertedMessage = null;
		}

		void nextIds(UUID... ids) {
			this.ids = new ArrayDeque<>(List.of(ids));
		}

		UUID nextId() {
			UUID id = ids.poll();
			if (id == null) {
				throw new IllegalStateException("no deterministic id left");
			}
			return id;
		}

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			return groupExists;
		}

		@Override
		public Optional<AiConversationMembershipContext> findMembership(UUID groupId, UUID userId) {
			return Optional.ofNullable(membership);
		}

		@Override
		public Optional<UUID> findWeekGroupId(UUID weekId) {
			return Optional.ofNullable(weekGroupId);
		}

		@Override
		public Optional<AiRetrospectiveReference> findRetrospectiveReference(UUID retrospectiveId) {
			return Optional.ofNullable(retrospectiveReference);
		}

		@Override
		public boolean insertConversation(AiConversation conversation) {
			insertedConversation = conversation;
			return true;
		}

		@Override
		public boolean existsConversation(UUID conversationId) {
			return true;
		}

		@Override
		public Optional<AiConversationMessageContext> findMessageContext(UUID conversationId, UUID userId) {
			return Optional.ofNullable(messageContext);
		}

		@Override
		public boolean insertMessage(AiConversationMessage message) {
			insertedMessage = message;
			return true;
		}

		@Override
		public List<AiConversationMessage> findMessages(UUID conversationId, AiConversationMessageCursor cursor, int limit) {
			return List.of();
		}

		@Override
		public AiConversationPromptContext findPromptContext(AiConversationMessageContext context, int recentMessageLimit) {
			return AiConversationPromptContext.empty();
		}

		@Override
		public boolean updateConversationSummary(UUID conversationId, String summary, Instant updatedAt) {
			return true;
		}
	}
}
