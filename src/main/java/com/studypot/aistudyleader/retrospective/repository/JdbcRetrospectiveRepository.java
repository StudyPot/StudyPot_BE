package com.studypot.aistudyleader.retrospective.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgressStatus;
import com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.retrospective.domain.Retrospective;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveAiContext;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveMembershipContext;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveProgress;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveStatus;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveTaskSummary;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveTriggerType;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcRetrospectiveRepository implements RetrospectiveRepository {

	private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
	};
	private static final TypeReference<List<com.studypot.aistudyleader.curriculum.domain.RetrospectiveQuestion>> QUESTION_LIST =
		new TypeReference<>() {
		};
	// 회고 질문이 비어 있는 주차(기능 추가 이전 생성된 기존 커리큘럼 등)에 제공하는 기본 질문셋.
	private static final List<com.studypot.aistudyleader.curriculum.domain.RetrospectiveQuestion> DEFAULT_QUESTIONS = List.of(
		new com.studypot.aistudyleader.curriculum.domain.RetrospectiveQuestion(
			"default-likert-1", "이번 주 목표한 학습을 충분히 달성했나요?",
			com.studypot.aistudyleader.curriculum.domain.RetrospectiveQuestionType.LIKERT_5),
		new com.studypot.aistudyleader.curriculum.domain.RetrospectiveQuestion(
			"default-likert-2", "이번 주 학습 난이도는 적절했나요?",
			com.studypot.aistudyleader.curriculum.domain.RetrospectiveQuestionType.LIKERT_5),
		new com.studypot.aistudyleader.curriculum.domain.RetrospectiveQuestion(
			"default-likert-3", "다음 주에도 현재 학습 페이스를 유지할 수 있을 것 같나요?",
			com.studypot.aistudyleader.curriculum.domain.RetrospectiveQuestionType.LIKERT_5),
		new com.studypot.aistudyleader.curriculum.domain.RetrospectiveQuestion(
			"default-text-1", "이번 주 학습에서 가장 도움이 된 점과 아쉬운 점을 자유롭게 적어주세요.",
			com.studypot.aistudyleader.curriculum.domain.RetrospectiveQuestionType.TEXT));
	private static final int PRIOR_RETROSPECTIVES_LIMIT = 3;

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	JdbcRetrospectiveRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public boolean existsCurriculumWeek(UUID weekId) {
		Objects.requireNonNull(weekId, "weekId must not be null");
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(RetrospectiveJdbcSql.EXISTS_CURRICULUM_WEEK, Boolean.class, uuid(weekId)));
	}

	@Override
	public Optional<RetrospectiveMembershipContext> findMembershipByWeekId(UUID weekId, UUID userId) {
		Objects.requireNonNull(weekId, "weekId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		return queryOne(RetrospectiveJdbcSql.SELECT_WEEK_MEMBERSHIP, this::mapMembership, uuid(weekId), uuid(userId));
	}

	@Override
	public Optional<RetrospectiveMembershipContext> findMembershipByGroupId(UUID groupId, UUID userId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		return queryOne(RetrospectiveJdbcSql.SELECT_GROUP_MEMBERSHIP, this::mapMembership, uuid(groupId), uuid(userId));
	}

	@Override
	public List<Retrospective> findMyRetrospectivesByGroup(UUID groupId, UUID memberId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		return jdbcTemplate.query(RetrospectiveJdbcSql.SELECT_MY_RETROSPECTIVES_BY_GROUP, this::mapRetrospective, uuid(groupId), uuid(memberId));
	}

	@Override
	public Optional<RetrospectiveProgress> findProgress(UUID weekId, UUID memberId) {
		Objects.requireNonNull(weekId, "weekId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		return queryOne(RetrospectiveJdbcSql.SELECT_PROGRESS, this::mapProgress, uuid(weekId), uuid(memberId));
	}

	@Override
	public Optional<Retrospective> findRetrospective(UUID progressId, UUID weekId, UUID memberId) {
		Objects.requireNonNull(progressId, "progressId must not be null");
		Objects.requireNonNull(weekId, "weekId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		return queryOne(RetrospectiveJdbcSql.SELECT_RETROSPECTIVE, this::mapRetrospective, uuid(progressId), uuid(weekId), uuid(memberId));
	}

	@Override
	public List<RetrospectiveTaskSummary> findTaskSummaries(UUID progressId, UUID weekId, UUID memberId) {
		Objects.requireNonNull(progressId, "progressId must not be null");
		Objects.requireNonNull(weekId, "weekId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		return jdbcTemplate.query(RetrospectiveJdbcSql.SELECT_TASK_SUMMARIES, this::mapTaskSummary, uuid(progressId), uuid(memberId), uuid(weekId));
	}

	@Override
	public RetrospectiveAiContext findAiContext(UUID groupId, UUID memberId, UUID weekId, UUID retrospectiveId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(weekId, "weekId must not be null");
		Objects.requireNonNull(retrospectiveId, "retrospectiveId must not be null");
		return new RetrospectiveAiContext(
			queryOne(RetrospectiveJdbcSql.SELECT_ONBOARDING_SUMMARY, this::mapOnboardingSummary, uuid(groupId), uuid(memberId))
				.orElse(Map.of("status", "NOT_AVAILABLE")),
			jdbcTemplate.query(RetrospectiveJdbcSql.SELECT_ACTIVE_RULE_SUMMARIES, this::mapRuleSummary, uuid(groupId)),
			jdbcTemplate.query(RetrospectiveJdbcSql.SELECT_RULE_VIOLATION_SUMMARIES, this::mapRuleViolationSummary, uuid(groupId), uuid(memberId)),
			jdbcTemplate.query(
				RetrospectiveJdbcSql.SELECT_PRIOR_RETROSPECTIVES,
				this::mapPriorRetrospective,
					uuid(memberId),
					uuid(retrospectiveId),
					uuid(weekId),
					PRIOR_RETROSPECTIVES_LIMIT
				),
			queryOne(
				RetrospectiveJdbcSql.SELECT_RETROSPECTIVE_CONVERSATION_SUMMARY,
				this::mapConversationSummary,
				uuid(retrospectiveId),
				uuid(memberId)
			).orElse(Map.of("status", "NOT_AVAILABLE"))
		);
	}

	@Override
	public boolean insertRetrospective(Retrospective retrospective) {
		Objects.requireNonNull(retrospective, "retrospective must not be null");
		return jdbcTemplate.update(
			RetrospectiveJdbcSql.INSERT_RETROSPECTIVE,
			uuid(retrospective.id()),
			uuid(retrospective.progressId()),
			uuid(retrospective.curriculumWeekId()),
			uuid(retrospective.memberId()),
			uuidOrNull(retrospective.llmUsageId()),
			retrospective.triggerType().name(),
			json(retrospective.inputSummary(), "retrospective input summary"),
			json(retrospective.aiFeedback(), "retrospective AI feedback"),
			json(retrospective.nextWeekAdjustment(), "retrospective next week adjustment"),
			retrospective.status().name(),
			timestamp(retrospective.requestedAt()),
			timestamp(retrospective.completedAt()),
			timestamp(retrospective.createdAt()),
			timestamp(retrospective.updatedAt())
		) == 1;
	}

	@Override
	public Optional<Retrospective> findRetrospectiveById(UUID retrospectiveId) {
		Objects.requireNonNull(retrospectiveId, "retrospectiveId must not be null");
		return queryOne(RetrospectiveJdbcSql.SELECT_RETROSPECTIVE_BY_ID, this::mapRetrospective, uuid(retrospectiveId));
	}

	@Override
	public boolean updateRetrospectiveResult(Retrospective retrospective) {
		Objects.requireNonNull(retrospective, "retrospective must not be null");
		return jdbcTemplate.update(
			RetrospectiveJdbcSql.UPDATE_RETROSPECTIVE_RESULT,
			uuidOrNull(retrospective.llmUsageId()),
			json(retrospective.aiFeedback(), "retrospective AI feedback"),
			json(retrospective.nextWeekAdjustment(), "retrospective next week adjustment"),
			retrospective.status().name(),
			timestamp(retrospective.completedAt()),
			timestamp(retrospective.updatedAt()),
			uuid(retrospective.id())
		) == 1;
	}

	@Override
	public boolean updateRetrospectiveAnswers(Retrospective retrospective) {
		Objects.requireNonNull(retrospective, "retrospective must not be null");
		return jdbcTemplate.update(
			RetrospectiveJdbcSql.UPDATE_RETROSPECTIVE_ANSWERS,
			json(retrospective.inputSummary(), "retrospective input summary"),
			retrospective.status().name(),
			timestamp(retrospective.completedAt()),
			timestamp(retrospective.updatedAt()),
			uuid(retrospective.id())
		) == 1;
	}

	@Override
	public List<com.studypot.aistudyleader.retrospective.domain.RetrospectiveWeekOverview> findRetrospectiveOverview(UUID groupId, UUID memberId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		return jdbcTemplate.query(
			RetrospectiveJdbcSql.SELECT_RETROSPECTIVE_OVERVIEW,
			this::mapWeekOverview,
			uuid(memberId),
			uuid(memberId),
			uuid(groupId)
		);
	}

	@Override
	public boolean isRetrospectiveWritable(UUID weekId, UUID memberId) {
		Objects.requireNonNull(weekId, "weekId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		List<Boolean> result = jdbcTemplate.query(
			RetrospectiveJdbcSql.SELECT_WEEK_WRITABILITY,
			(resultSet, rowNumber) -> com.studypot.aistudyleader.retrospective.domain.RetrospectiveWeekOverview.unlocked(
				resultSet.getString("status"),
				resultSet.getLong("required_total"),
				resultSet.getLong("required_done"),
				resultSet.getBoolean("report_posted")
			),
			uuid(memberId),
			uuid(weekId)
		);
		return !result.isEmpty() && Boolean.TRUE.equals(result.get(0));
	}

	private com.studypot.aistudyleader.retrospective.domain.RetrospectiveWeekOverview mapWeekOverview(ResultSet resultSet, int rowNumber)
		throws SQLException {
		long requiredTotal = resultSet.getLong("required_total");
		long requiredDone = resultSet.getLong("required_done");
		String status = resultSet.getString("status");
		boolean reportPosted = resultSet.getBoolean("report_posted");
		boolean unlocked = com.studypot.aistudyleader.retrospective.domain.RetrospectiveWeekOverview
			.unlocked(status, requiredTotal, requiredDone, reportPosted);
		return new com.studypot.aistudyleader.retrospective.domain.RetrospectiveWeekOverview(
			UuidBinary.fromBytes(resultSet.getBytes("week_id")),
			resultSet.getInt("week_number"),
			status,
			unlocked,
			resultSet.getLong("answered_count") > 0,
			readQuestions(resultSet.getString("retrospective_questions"))
		);
	}

	private List<com.studypot.aistudyleader.curriculum.domain.RetrospectiveQuestion> readQuestions(String value) {
		if (value == null || value.isBlank()) {
			// 질문 미생성 주차는 기본 질문셋으로 대체(기존 그룹도 회고 작성 가능).
			return DEFAULT_QUESTIONS;
		}
		try {
			List<com.studypot.aistudyleader.curriculum.domain.RetrospectiveQuestion> parsed = objectMapper.readValue(value, QUESTION_LIST);
			return parsed.isEmpty() ? DEFAULT_QUESTIONS : parsed;
		} catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
			throw new RetrospectivePersistenceException("failed to read retrospective questions.", exception);
		}
	}

	private RetrospectiveMembershipContext mapMembership(ResultSet resultSet, int rowNumber) throws SQLException {
		return new RetrospectiveMembershipContext(
			UuidBinary.fromBytes(resultSet.getBytes("group_id")),
			UuidBinary.fromBytes(resultSet.getBytes("member_id")),
			StudyGroupStatus.valueOf(resultSet.getString("group_status")),
			GroupMemberPermission.valueOf(resultSet.getString("permission")),
			GroupMemberStatus.valueOf(resultSet.getString("member_status"))
		);
	}

	private RetrospectiveProgress mapProgress(ResultSet resultSet, int rowNumber) throws SQLException {
		return new RetrospectiveProgress(
			UuidBinary.fromBytes(resultSet.getBytes("id")),
			UuidBinary.fromBytes(resultSet.getBytes("curriculum_week_id")),
			UuidBinary.fromBytes(resultSet.getBytes("member_id")),
			MemberWeekProgressStatus.valueOf(resultSet.getString("status")),
			instant(resultSet.getTimestamp("started_at")),
			instant(resultSet.getTimestamp("due_at")),
			instant(resultSet.getTimestamp("completed_at")),
			resultSet.getString("completion_note"),
			resultSet.getString("incomplete_reason"),
			instant(resultSet.getTimestamp("reason_submitted_at"))
		);
	}

	private Retrospective mapRetrospective(ResultSet resultSet, int rowNumber) throws SQLException {
		return new Retrospective(
			UuidBinary.fromBytes(resultSet.getBytes("id")),
			UuidBinary.fromBytes(resultSet.getBytes("progress_id")),
			UuidBinary.fromBytes(resultSet.getBytes("curriculum_week_id")),
			UuidBinary.fromBytes(resultSet.getBytes("member_id")),
			uuid(resultSet.getBytes("llm_usage_id")),
			RetrospectiveTriggerType.valueOf(resultSet.getString("trigger_type")),
			readNullableMap(resultSet.getString("input_summary"), "retrospective input summary"),
			readNullableMap(resultSet.getString("ai_feedback"), "retrospective AI feedback"),
			readNullableMap(resultSet.getString("next_week_adjustment"), "retrospective next week adjustment"),
			RetrospectiveStatus.valueOf(resultSet.getString("status")),
			instant(resultSet.getTimestamp("requested_at")),
			instant(resultSet.getTimestamp("completed_at")),
			instant(resultSet.getTimestamp("created_at")),
			instant(resultSet.getTimestamp("updated_at"))
		);
	}

	private RetrospectiveTaskSummary mapTaskSummary(ResultSet resultSet, int rowNumber) throws SQLException {
		return new RetrospectiveTaskSummary(
			UuidBinary.fromBytes(resultSet.getBytes("task_id")),
			resultSet.getInt("display_order"),
			WeeklyTaskType.valueOf(resultSet.getString("task_type")),
			resultSet.getString("title"),
			resultSet.getBoolean("required"),
			instant(resultSet.getTimestamp("due_at")),
			TaskCompletionStatus.valueOf(resultSet.getString("completion_status")),
			instant(resultSet.getTimestamp("completed_at")),
			resultSet.getString("completion_note"),
			resultSet.getString("incomplete_reason"),
			instant(resultSet.getTimestamp("reason_submitted_at"))
		);
	}

	private Map<String, Object> mapOnboardingSummary(ResultSet resultSet, int rowNumber) throws SQLException {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", UuidBinary.fromBytes(resultSet.getBytes("id")).toString());
		result.put("status", resultSet.getString("status"));
		result.put("keywordSkillLevels", readNullableMap(resultSet.getString("keyword_skill_levels"), "onboarding keyword skill levels"));
		result.put("taskPreferences", readNullableMap(resultSet.getString("task_preferences"), "onboarding task preferences"));
		putText(result, "additionalNote", resultSet.getString("additional_note"));
		putInstant(result, "submittedAt", instant(resultSet.getTimestamp("submitted_at")));
		return result;
	}

	private Map<String, Object> mapRuleSummary(ResultSet resultSet, int rowNumber) throws SQLException {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", UuidBinary.fromBytes(resultSet.getBytes("id")).toString());
		result.put("ruleType", resultSet.getString("rule_type"));
		result.put("config", readNullableMap(resultSet.getString("config"), "group rule config"));
		putText(result, "description", resultSet.getString("description"));
		result.put("active", resultSet.getBoolean("is_active"));
		return result;
	}

	private Map<String, Object> mapRuleViolationSummary(ResultSet resultSet, int rowNumber) throws SQLException {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", UuidBinary.fromBytes(resultSet.getBytes("id")).toString());
		result.put("ruleId", UuidBinary.fromBytes(resultSet.getBytes("rule_id")).toString());
		result.put("ruleType", resultSet.getString("rule_type"));
		putUuid(result, "taskCompletionId", uuid(resultSet.getBytes("task_completion_id")));
		result.put("details", readNullableMap(resultSet.getString("details"), "rule violation details"));
		result.put("status", resultSet.getString("status"));
		putInstant(result, "resolvedAt", instant(resultSet.getTimestamp("resolved_at")));
		putText(result, "resolvedNote", resultSet.getString("resolved_note"));
		putInstant(result, "occurredAt", instant(resultSet.getTimestamp("occurred_at")));
		return result;
	}

	private Map<String, Object> mapPriorRetrospective(ResultSet resultSet, int rowNumber) throws SQLException {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", UuidBinary.fromBytes(resultSet.getBytes("id")).toString());
		result.put("weekId", UuidBinary.fromBytes(resultSet.getBytes("curriculum_week_id")).toString());
		result.put("status", resultSet.getString("status"));
		result.put("aiFeedback", readNullableMap(resultSet.getString("ai_feedback"), "prior retrospective AI feedback"));
		result.put(
			"nextWeekAdjustment",
			readNullableMap(resultSet.getString("next_week_adjustment"), "prior retrospective next week adjustment")
		);
		putInstant(result, "requestedAt", instant(resultSet.getTimestamp("requested_at")));
		putInstant(result, "completedAt", instant(resultSet.getTimestamp("completed_at")));
		return result;
	}

	private Map<String, Object> mapConversationSummary(ResultSet resultSet, int rowNumber) throws SQLException {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("status", "AVAILABLE");
		result.put("conversationId", UuidBinary.fromBytes(resultSet.getBytes("id")).toString());
		result.put("conversationStatus", resultSet.getString("status"));
		result.put("summary", resultSet.getString("summary"));
		putInstant(result, "openedAt", instant(resultSet.getTimestamp("opened_at")));
		putInstant(result, "closedAt", instant(resultSet.getTimestamp("closed_at")));
		return result;
	}

	private <T> Optional<T> queryOne(String sql, ThrowingRowMapper<T> mapper, Object... args) {
		List<T> results = jdbcTemplate.query(sql, (resultSet, rowNumber) -> mapper.map(resultSet, rowNumber), args);
		return results.stream().findFirst();
	}

	private String json(Map<String, Object> value, String fieldName) {
		try {
			return objectMapper.writeValueAsString(value == null ? Map.of() : value);
		} catch (JsonProcessingException exception) {
			throw new RetrospectivePersistenceException(fieldName + " could not be serialized.", exception);
		}
	}

	private Map<String, Object> readNullableMap(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			return Map.of();
		}
		try {
			return objectMapper.readValue(value, OBJECT_MAP);
		} catch (JsonProcessingException exception) {
			throw new RetrospectivePersistenceException(fieldName + " could not be deserialized.", exception);
		}
	}

	private static byte[] uuid(UUID uuid) {
		return UuidBinary.toBytes(uuid);
	}

	private static byte[] uuidOrNull(UUID uuid) {
		return uuid == null ? null : UuidBinary.toBytes(uuid);
	}

	private static UUID uuid(byte[] bytes) {
		return bytes == null ? null : UuidBinary.fromBytes(bytes);
	}

	private static Timestamp timestamp(Instant instant) {
		return instant == null ? null : Timestamp.from(instant);
	}

	private static Instant instant(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toInstant();
	}

	private static void putText(Map<String, Object> target, String key, String value) {
		if (value != null && !value.isBlank()) {
			target.put(key, value.strip());
		}
	}

	private static void putInstant(Map<String, Object> target, String key, Instant value) {
		if (value != null) {
			target.put(key, value.toString());
		}
	}

	private static void putUuid(Map<String, Object> target, String key, UUID value) {
		if (value != null) {
			target.put(key, value.toString());
		}
	}

	private interface ThrowingRowMapper<T> {

		T map(ResultSet resultSet, int rowNumber) throws SQLException;
	}
}
