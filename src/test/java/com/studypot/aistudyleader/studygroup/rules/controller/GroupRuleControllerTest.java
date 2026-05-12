package com.studypot.aistudyleader.studygroup.rules.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.rules.domain.GroupRule;
import com.studypot.aistudyleader.studygroup.rules.domain.GroupRuleMembership;
import com.studypot.aistudyleader.studygroup.rules.domain.GroupRuleType;
import com.studypot.aistudyleader.studygroup.rules.domain.RuleViolation;
import com.studypot.aistudyleader.studygroup.rules.domain.RuleViolationStatus;
import com.studypot.aistudyleader.studygroup.rules.domain.RuleViolationType;
import com.studypot.aistudyleader.studygroup.rules.repository.GroupRuleRepository;
import com.studypot.aistudyleader.studygroup.rules.service.GroupRuleService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
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

@SpringBootTest(classes = {AiStudyLeaderApplication.class, GroupRuleControllerTest.TestGroupRuleBeans.class})
@AutoConfigureMockMvc
class GroupRuleControllerTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000005201");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000005202");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000005203");
	private static final UUID RULE_ID = UUID.fromString("018f0000-0000-7000-8000-000000005204");
	private static final UUID VIOLATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000005205");
	private static final UUID TASK_COMPLETION_ID = UUID.fromString("018f0000-0000-7000-8000-000000005206");
	private static final String RULE_PATH = ApiPaths.V1 + "/groups/" + GROUP_ID + "/rules";
	private static final String VIOLATION_PATH = ApiPaths.V1 + "/groups/" + GROUP_ID + "/rule-violations";

	private final MockMvc mockMvc;
	private final MutableGroupRuleRepository repository;

	@Autowired
	GroupRuleControllerTest(MockMvc mockMvc, MutableGroupRuleRepository repository) {
		this.mockMvc = mockMvc;
		this.repository = repository;
	}

	@BeforeEach
	void setUp() {
		repository.reset();
	}

	@Test
	void saveRuleRequiresAuthentication() throws Exception {
		mockMvc.perform(put(RULE_PATH + "/TASK_DEADLINE")
				.with(xsrf("rule-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"config":{"dueTime":"23:59"},"description":"금요일 자정 전까지 제출","active":true}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void saveRuleValidatesConfig() throws Exception {
		mockMvc.perform(put(RULE_PATH + "/TASK_DEADLINE")
				.with(user(USER_ID.toString()))
				.with(xsrf("rule-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"config":{},"description":"빈 설정","active":true}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("config"));
	}

	@Test
	void saveRuleReturnsSavedRule() throws Exception {
		mockMvc.perform(put(RULE_PATH + "/TASK_DEADLINE")
				.with(user(USER_ID.toString()))
				.with(xsrf("rule-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"config":{"dueDayOfWeek":5,"dueTime":"23:59","timezone":"Asia/Seoul"},"description":"금요일 자정 전까지 제출","active":true}
					"""))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(RULE_ID.toString()))
			.andExpect(jsonPath("$.groupId").value(GROUP_ID.toString()))
			.andExpect(jsonPath("$.ruleType").value("TASK_DEADLINE"))
			.andExpect(jsonPath("$.config.dueTime").value("23:59"))
			.andExpect(jsonPath("$.description").value("금요일 자정 전까지 제출"))
			.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void listRulesRejectsNonMember() throws Exception {
		repository.membership = null;
		repository.groupExists = true;

		mockMvc.perform(get(RULE_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void listRulesReturnsSavedRules() throws Exception {
		repository.rules.put(RULE_ID, rule(GroupRuleType.CUSTOM_NOTE, Map.of("memo", "지각 시 공유"), true));

		mockMvc.perform(get(RULE_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(RULE_ID.toString()))
			.andExpect(jsonPath("$[0].ruleType").value("CUSTOM_NOTE"))
			.andExpect(jsonPath("$[0].config.memo").value("지각 시 공유"));
	}

	@Test
	void deactivateRuleReturnsNoContent() throws Exception {
		repository.rules.put(RULE_ID, rule(GroupRuleType.RETROSPECTIVE_REQUIRED, Map.of("required", true), true));

		mockMvc.perform(patch(RULE_PATH + "/" + RULE_ID + "/deactivate")
				.with(user(USER_ID.toString()))
				.with(xsrf("rule-xsrf")))
			.andExpect(status().isNoContent());
	}

	@Test
	void deleteRuleReturnsNoContent() throws Exception {
		repository.rules.put(RULE_ID, rule(GroupRuleType.RETROSPECTIVE_REQUIRED, Map.of("required", true), true));

		mockMvc.perform(delete(RULE_PATH + "/" + RULE_ID)
				.with(user(USER_ID.toString()))
				.with(xsrf("rule-xsrf")))
			.andExpect(status().isNoContent());
	}

	@Test
	void recordViolationReturnsOpenViolation() throws Exception {
		repository.rules.put(RULE_ID, rule(GroupRuleType.TASK_DEADLINE, Map.of("dueTime", "23:59"), true));
		repository.ids = new ArrayDeque<>(List.of(VIOLATION_ID));

		mockMvc.perform(post(VIOLATION_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("violation-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "ruleId": "%s",
					  "memberId": "%s",
					  "taskCompletionId": "%s",
					  "violationType": "INCOMPLETE_REASON_MISSING",
					  "details": {"reason": "마감 후 미완료 사유 없음"},
					  "occurredAt": "2026-05-12T00:59:00Z"
					}
					""".formatted(RULE_ID, MEMBER_ID, TASK_COMPLETION_ID)))
			.andExpect(status().isCreated())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(VIOLATION_ID.toString()))
			.andExpect(jsonPath("$.ruleId").value(RULE_ID.toString()))
			.andExpect(jsonPath("$.memberId").value(MEMBER_ID.toString()))
			.andExpect(jsonPath("$.taskCompletionId").value(TASK_COMPLETION_ID.toString()))
			.andExpect(jsonPath("$.violationType").value("INCOMPLETE_REASON_MISSING"))
			.andExpect(jsonPath("$.status").value("OPEN"))
			.andExpect(jsonPath("$.details.reason").value("마감 후 미완료 사유 없음"));
	}

	@Test
	void resolveViolationReturnsResolvedViolation() throws Exception {
		repository.violations.put(VIOLATION_ID, violation(RuleViolationStatus.OPEN));

		mockMvc.perform(patch(VIOLATION_PATH + "/" + VIOLATION_ID + "/resolve")
				.with(user(USER_ID.toString()))
				.with(xsrf("violation-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"note":"미완료 사유를 제출해서 해결"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(VIOLATION_ID.toString()))
			.andExpect(jsonPath("$.status").value("RESOLVED"))
			.andExpect(jsonPath("$.resolvedAt").value(TestGroupRuleBeans.NOW.toString()))
			.andExpect(jsonPath("$.resolvedNote").value("미완료 사유를 제출해서 해결"));
	}

	@Test
	void waiveViolationRejectsAlreadyHandledViolation() throws Exception {
		repository.violations.put(VIOLATION_ID, violation(RuleViolationStatus.RESOLVED));

		mockMvc.perform(patch(VIOLATION_PATH + "/" + VIOLATION_ID + "/waive")
				.with(user(USER_ID.toString()))
				.with(xsrf("violation-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"note":"운영상 면제"}
					"""))
			.andExpect(status().isConflict())
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

	private static GroupRule rule(GroupRuleType type, Map<String, Object> config, boolean active) {
		return new GroupRule(
			RULE_ID,
			GROUP_ID,
			USER_ID,
			type,
			config,
			"테스트 규칙",
			active,
			TestGroupRuleBeans.NOW,
			TestGroupRuleBeans.NOW,
			Optional.empty()
		);
	}

	private static RuleViolation violation(RuleViolationStatus status) {
		return new RuleViolation(
			VIOLATION_ID,
			RULE_ID,
			MEMBER_ID,
			Optional.of(TASK_COMPLETION_ID),
			RuleViolationType.INCOMPLETE_REASON_MISSING,
			Map.of("reason", "missing"),
			status,
			Optional.empty(),
			Optional.empty(),
			TestGroupRuleBeans.NOW.minusSeconds(60),
			TestGroupRuleBeans.NOW.minusSeconds(30)
		);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestGroupRuleBeans {

		private static final Instant NOW = Instant.parse("2026-05-12T01:00:00Z");

		@Bean
		@Primary
		MutableGroupRuleRepository mutableGroupRuleRepository() {
			return new MutableGroupRuleRepository();
		}

		@Bean
		@Primary
		GroupRuleService testGroupRuleService(MutableGroupRuleRepository repository) {
			return new GroupRuleService(repository, Clock.fixed(NOW, ZoneOffset.UTC), repository::nextId);
		}
	}

	static final class MutableGroupRuleRepository implements GroupRuleRepository {

		private boolean groupExists;
		private GroupRuleMembership membership;
		private GroupRuleMembership targetMembership;
		private final Map<UUID, GroupRule> rules = new LinkedHashMap<>();
		private final Map<UUID, RuleViolation> violations = new LinkedHashMap<>();
		private Deque<UUID> ids;

		void reset() {
			groupExists = true;
			membership = new GroupRuleMembership(GROUP_ID, MEMBER_ID, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
			targetMembership = membership;
			rules.clear();
			violations.clear();
			ids = new ArrayDeque<>(List.of(RULE_ID, VIOLATION_ID));
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
		public Optional<GroupRuleMembership> findMembership(UUID groupId, UUID userId) {
			return Optional.ofNullable(membership);
		}

		@Override
		public Optional<GroupRuleMembership> findMemberById(UUID groupId, UUID memberId) {
			return Optional.ofNullable(targetMembership);
		}

		@Override
		public Optional<GroupRule> findRuleByGroupAndTypeForUpdate(UUID groupId, GroupRuleType ruleType) {
			return rules.values().stream()
				.filter(rule -> rule.ruleType() == ruleType)
				.findFirst();
		}

		@Override
		public Optional<GroupRule> findRuleById(UUID groupId, UUID ruleId) {
			return Optional.ofNullable(rules.get(ruleId));
		}

		@Override
		public boolean taskCompletionBelongsToMember(UUID taskCompletionId, UUID memberId) {
			return true;
		}

		@Override
		public List<GroupRule> findRulesByGroupId(UUID groupId) {
			return new ArrayList<>(rules.values());
		}

		@Override
		public void insertRule(GroupRule rule) {
			rules.put(rule.id(), rule);
		}

		@Override
		public void updateRule(GroupRule rule) {
			rules.put(rule.id(), rule);
		}

		@Override
		public boolean deactivateRule(UUID groupId, UUID ruleId, Instant updatedAt) {
			GroupRule rule = rules.get(ruleId);
			if (rule == null) {
				return false;
			}
			rules.put(ruleId, rule.update(rule.config(), rule.description(), false, updatedAt));
			return true;
		}

		@Override
		public boolean softDeleteRule(UUID groupId, UUID ruleId, Instant deletedAt) {
			return rules.remove(ruleId) != null;
		}

		@Override
		public Optional<RuleViolation> findViolationById(UUID groupId, UUID violationId) {
			return Optional.ofNullable(violations.get(violationId));
		}

		@Override
		public List<RuleViolation> findViolationsByGroupId(UUID groupId) {
			return new ArrayList<>(violations.values());
		}

		@Override
		public void insertViolation(RuleViolation violation) {
			violations.put(violation.id(), violation);
		}

		@Override
		public boolean updateViolationStatus(RuleViolation violation) {
			if (!violations.containsKey(violation.id())) {
				return false;
			}
			violations.put(violation.id(), violation);
			return true;
		}
	}
}
