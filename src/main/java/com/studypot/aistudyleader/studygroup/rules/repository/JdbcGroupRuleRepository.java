package com.studypot.aistudyleader.studygroup.rules.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.rules.domain.GroupRule;
import com.studypot.aistudyleader.studygroup.rules.domain.GroupRuleMembership;
import com.studypot.aistudyleader.studygroup.rules.domain.GroupRuleType;
import com.studypot.aistudyleader.studygroup.rules.domain.RuleViolation;
import com.studypot.aistudyleader.studygroup.rules.domain.RuleViolationStatus;
import com.studypot.aistudyleader.studygroup.rules.domain.RuleViolationType;
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

class JdbcGroupRuleRepository implements GroupRuleRepository {

	private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
	};
	private static final String VIOLATION_TYPE_KEY = "violationType";

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	JdbcGroupRuleRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public boolean existsStudyGroup(UUID groupId) {
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(GroupRuleJdbcSql.EXISTS_STUDY_GROUP, Boolean.class, uuid(groupId)));
	}

	@Override
	public Optional<GroupRuleMembership> findMembership(UUID groupId, UUID userId) {
		return queryOne(GroupRuleJdbcSql.SELECT_MEMBERSHIP, this::mapMembership, uuid(groupId), uuid(userId));
	}

	@Override
	public Optional<GroupRuleMembership> findMemberById(UUID groupId, UUID memberId) {
		return queryOne(GroupRuleJdbcSql.SELECT_MEMBER_BY_ID, this::mapMembership, uuid(groupId), uuid(memberId));
	}

	@Override
	public Optional<GroupRule> findRuleByGroupAndTypeForUpdate(UUID groupId, GroupRuleType ruleType) {
		return queryOne(GroupRuleJdbcSql.SELECT_RULE_BY_GROUP_TYPE_FOR_UPDATE, this::mapRule, uuid(groupId), ruleType.name());
	}

	@Override
	public Optional<GroupRule> findRuleById(UUID groupId, UUID ruleId) {
		return queryOne(GroupRuleJdbcSql.SELECT_RULE_BY_ID, this::mapRule, uuid(groupId), uuid(ruleId));
	}

	@Override
	public boolean existsTaskCompletion(UUID taskCompletionId) {
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
			GroupRuleJdbcSql.EXISTS_TASK_COMPLETION,
			Boolean.class,
			uuid(taskCompletionId)
		));
	}

	@Override
	public boolean taskCompletionBelongsToMember(UUID taskCompletionId, UUID memberId) {
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
			GroupRuleJdbcSql.EXISTS_TASK_COMPLETION_FOR_MEMBER,
			Boolean.class,
			uuid(taskCompletionId),
			uuid(memberId)
		));
	}

	@Override
	public List<GroupRule> findRulesByGroupId(UUID groupId) {
		return jdbcTemplate.query(GroupRuleJdbcSql.SELECT_RULES_BY_GROUP_ID, this::mapRule, uuid(groupId));
	}

	@Override
	public boolean insertRule(GroupRule rule) {
		return jdbcTemplate.update(
			GroupRuleJdbcSql.INSERT_RULE,
			uuid(rule.id()),
			uuid(rule.groupId()),
			uuid(rule.createdBy()),
			rule.ruleType().name(),
			json(rule.config(), "group rule config"),
			rule.description(),
			rule.active(),
			timestamp(rule.createdAt()),
			timestamp(rule.updatedAt())
		) > 0;
	}

	@Override
	public boolean updateRule(GroupRule rule) {
		return jdbcTemplate.update(
			GroupRuleJdbcSql.UPDATE_RULE,
			json(rule.config(), "group rule config"),
			rule.description(),
			rule.active(),
			timestamp(rule.updatedAt()),
			uuid(rule.groupId()),
			uuid(rule.id())
		) > 0;
	}

	@Override
	public boolean deactivateRule(UUID groupId, UUID ruleId, Instant updatedAt) {
		return jdbcTemplate.update(
			GroupRuleJdbcSql.DEACTIVATE_RULE,
			timestamp(updatedAt),
			uuid(groupId),
			uuid(ruleId)
		) > 0;
	}

	@Override
	public boolean softDeleteRule(UUID groupId, UUID ruleId, Instant deletedAt) {
		return jdbcTemplate.update(
			GroupRuleJdbcSql.SOFT_DELETE_RULE,
			timestamp(deletedAt),
			timestamp(deletedAt),
			uuid(groupId),
			uuid(ruleId)
		) > 0;
	}

	@Override
	public Optional<RuleViolation> findViolationById(UUID groupId, UUID violationId) {
		return queryOne(GroupRuleJdbcSql.SELECT_VIOLATION_BY_ID, this::mapViolation, uuid(groupId), uuid(violationId));
	}

	@Override
	public List<RuleViolation> findViolationsByGroupId(UUID groupId) {
		return jdbcTemplate.query(GroupRuleJdbcSql.SELECT_VIOLATIONS_BY_GROUP_ID, this::mapViolation, uuid(groupId));
	}

	@Override
	public void insertViolation(RuleViolation violation) {
		jdbcTemplate.update(
			GroupRuleJdbcSql.INSERT_VIOLATION,
			uuid(violation.id()),
			uuid(violation.ruleId()),
			uuid(violation.memberId()),
			uuidOrNull(violation.taskCompletionId().orElse(null)),
			json(violationDetails(violation), "rule violation details"),
			violation.status().name(),
			timestamp(violation.resolvedAt().orElse(null)),
			violation.resolvedNote().orElse(null),
			timestamp(violation.occurredAt()),
			timestamp(violation.createdAt())
		);
	}

	@Override
	public boolean updateViolationStatus(RuleViolation violation) {
		return jdbcTemplate.update(
			GroupRuleJdbcSql.UPDATE_VIOLATION_STATUS,
			violation.status().name(),
			timestamp(violation.resolvedAt().orElse(null)),
			violation.resolvedNote().orElse(null),
			uuid(violation.id())
		) > 0;
	}

	GroupRuleMembership mapMembership(ResultSet resultSet, int rowNumber) throws SQLException {
		return new GroupRuleMembership(
			UuidBinary.fromBytes(resultSet.getBytes("group_id")),
			UuidBinary.fromBytes(resultSet.getBytes("member_id")),
			GroupMemberPermission.valueOf(resultSet.getString("permission")),
			GroupMemberStatus.valueOf(resultSet.getString("member_status"))
		);
	}

	GroupRule mapRule(ResultSet resultSet, int rowNumber) throws SQLException {
		return new GroupRule(
			UuidBinary.fromBytes(resultSet.getBytes("id")),
			UuidBinary.fromBytes(resultSet.getBytes("group_id")),
			UuidBinary.fromBytes(resultSet.getBytes("created_by")),
			GroupRuleType.valueOf(resultSet.getString("rule_type")),
			readNullableMap(resultSet.getString("config"), "group rule config"),
			resultSet.getString("description"),
			resultSet.getBoolean("is_active"),
			instant(resultSet.getTimestamp("created_at")),
			instant(resultSet.getTimestamp("updated_at")),
			Optional.ofNullable(instant(resultSet.getTimestamp("deleted_at")))
		);
	}

	RuleViolation mapViolation(ResultSet resultSet, int rowNumber) throws SQLException {
		Map<String, Object> details = readNullableMap(resultSet.getString("details"), "rule violation details");
		RuleViolationType violationType = parseViolationType(details);
		Map<String, Object> detailsWithoutType = new LinkedHashMap<>(details);
		detailsWithoutType.remove(VIOLATION_TYPE_KEY);
		return new RuleViolation(
			UuidBinary.fromBytes(resultSet.getBytes("id")),
			UuidBinary.fromBytes(resultSet.getBytes("rule_id")),
			UuidBinary.fromBytes(resultSet.getBytes("member_id")),
			Optional.ofNullable(uuid(resultSet.getBytes("task_completion_id"))),
			violationType,
			detailsWithoutType,
			RuleViolationStatus.valueOf(resultSet.getString("status")),
			Optional.ofNullable(instant(resultSet.getTimestamp("resolved_at"))),
			Optional.ofNullable(resultSet.getString("resolved_note")),
			instant(resultSet.getTimestamp("occurred_at")),
			instant(resultSet.getTimestamp("created_at"))
		);
	}

	private <T> Optional<T> queryOne(String sql, ThrowingRowMapper<T> mapper, Object... args) {
		List<T> results = jdbcTemplate.query(sql, (resultSet, rowNumber) -> mapper.map(resultSet, rowNumber), args);
		return results.stream().findFirst();
	}

	private Map<String, Object> violationDetails(RuleViolation violation) {
		if (violation.details().containsKey(VIOLATION_TYPE_KEY)) {
			throw new IllegalArgumentException("details must not contain reserved key: " + VIOLATION_TYPE_KEY);
		}
		Map<String, Object> details = new LinkedHashMap<>();
		details.put(VIOLATION_TYPE_KEY, violation.violationType().name());
		details.putAll(violation.details());
		return details;
	}

	private RuleViolationType parseViolationType(Map<String, Object> details) {
		Object value = details.get(VIOLATION_TYPE_KEY);
		if (value == null || value.toString().isBlank()) {
			return RuleViolationType.CUSTOM;
		}
		try {
			return RuleViolationType.valueOf(value.toString());
		} catch (IllegalArgumentException exception) {
			throw new GroupRulePersistenceException("rule violation details contains invalid violationType: " + value, exception);
		}
	}

	private String json(Object value, String fieldName) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new GroupRulePersistenceException(fieldName + " could not be serialized.", exception);
		}
	}

	private Map<String, Object> readNullableMap(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			return Map.of();
		}
		try {
			return objectMapper.readValue(value, OBJECT_MAP);
		} catch (JsonProcessingException exception) {
			throw new GroupRulePersistenceException(fieldName + " could not be deserialized.", exception);
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

	@FunctionalInterface
	private interface ThrowingRowMapper<T> {

		T map(ResultSet resultSet, int rowNumber) throws SQLException;
	}
}
