package com.studypot.aistudyleader.llm.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.llm.admin.AdminLlmUsageFilter;
import com.studypot.aistudyleader.llm.admin.AdminLlmUsageRow;
import com.studypot.aistudyleader.llm.admin.AdminLlmUsageSummary;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsage;
import com.studypot.aistudyleader.llm.domain.LlmUsageAccessContext;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.repository.LlmUsageRepository;
import com.studypot.aistudyleader.llm.service.LlmUsageService;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.math.BigDecimal;
import java.time.Instant;
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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = {AiStudyLeaderApplication.class, LlmUsageControllerTest.TestLlmUsageBeans.class})
@AutoConfigureMockMvc
class LlmUsageControllerTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000007301");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000007302");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000007303");
	private static final UUID USAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000007304");
	private static final String USAGE_PATH = ApiPaths.V1 + "/groups/" + GROUP_ID + "/llm-usage";

	private final MockMvc mockMvc;
	private final MutableLlmUsageRepository repository;

	@Autowired
	LlmUsageControllerTest(MockMvc mockMvc, MutableLlmUsageRepository repository) {
		this.mockMvc = mockMvc;
		this.repository = repository;
	}

	@BeforeEach
	void setUp() {
		repository.reset();
	}

	@Test
	void listGroupUsageRequiresAuthentication() throws Exception {
		mockMvc.perform(get(USAGE_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void listGroupUsageReturnsOwnerVisibleLogs() throws Exception {
		mockMvc.perform(get(USAGE_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$[0].id").value(USAGE_ID.toString()))
			.andExpect(jsonPath("$[0].purpose").value("TEAM_LEAD_CHAT"))
			.andExpect(jsonPath("$[0].provider").value("OPENAI"))
			.andExpect(jsonPath("$[0].model").value("gpt-4.1-mini"))
			.andExpect(jsonPath("$[0].inputTokens").value(120))
			.andExpect(jsonPath("$[0].outputTokens").value(45))
			.andExpect(jsonPath("$[0].status").value("SUCCESS"));
	}

	@Test
	void listGroupUsageReturnsForbiddenForNonOwner() throws Exception {
		repository.accessContext = new LlmUsageAccessContext(
			GROUP_ID,
			MEMBER_ID,
			StudyGroupStatus.ACTIVE,
			GroupMemberPermission.MEMBER,
			GroupMemberStatus.ACTIVE
		);

		mockMvc.perform(get(USAGE_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Forbidden"));
	}

	@Test
	void listGroupUsageReturnsForbiddenForLeftOwner() throws Exception {
		repository.accessContext = new LlmUsageAccessContext(
			GROUP_ID,
			MEMBER_ID,
			StudyGroupStatus.ACTIVE,
			GroupMemberPermission.OWNER,
			GroupMemberStatus.LEFT
		);

		mockMvc.perform(get(USAGE_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Forbidden"));
	}

	@Test
	void listGroupUsageReturnsNotFoundForMissingGroup() throws Exception {
		repository.accessContext = null;
		repository.groupExists = false;

		mockMvc.perform(get(USAGE_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestLlmUsageBeans {

		@Bean
		MutableLlmUsageRepository mutableLlmUsageRepository() {
			return new MutableLlmUsageRepository();
		}

		@Bean
		@Primary
		LlmUsageService testLlmUsageService(MutableLlmUsageRepository repository) {
			return new LlmUsageService(repository);
		}
	}

	static final class MutableLlmUsageRepository implements LlmUsageRepository {

		private LlmUsageAccessContext accessContext;
		private boolean groupExists;
		private List<LlmUsage> usages;

		void reset() {
			accessContext = new LlmUsageAccessContext(
				GROUP_ID,
				MEMBER_ID,
				StudyGroupStatus.ACTIVE,
				GroupMemberPermission.OWNER,
				GroupMemberStatus.ACTIVE
			);
			groupExists = true;
			usages = List.of(usage());
		}

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			return groupExists;
		}

		@Override
		public Optional<LlmUsageAccessContext> findAccessContext(UUID groupId, UUID userId) {
			return Optional.ofNullable(accessContext);
		}

		@Override
		public boolean insertLlmUsage(LlmUsage usage) {
			return true;
		}

		@Override
		public List<LlmUsage> findGroupUsage(UUID groupId, int limit) {
			return usages;
		}

		@Override
		public List<LlmUsage> findUserUsage(UUID userId, int limit) {
			return List.of();
		}

		@Override
		public Optional<String> findUserEmail(UUID userId) {
			return Optional.empty();
		}

		@Override
		public List<AdminLlmUsageRow> findAdminUsage(AdminLlmUsageFilter filter) {
			return List.of();
		}

		@Override
		public AdminLlmUsageSummary summarizeAdminUsage(AdminLlmUsageFilter filter) {
			return AdminLlmUsageSummary.EMPTY;
		}

		private static LlmUsage usage() {
			return LlmUsage.record(
				USAGE_ID,
				USER_ID,
				GROUP_ID,
				LlmUsagePurpose.TEAM_LEAD_CHAT,
				LlmProvider.OPENAI,
				"gpt-4.1-mini",
				120,
				45,
				new BigDecimal("0.000321"),
				230,
				LlmUsageStatus.SUCCESS,
				null,
				Map.of("source", "chat"),
				"assistant response summary",
				Instant.parse("2026-05-13T02:40:00Z")
			);
		}
	}
}
