package com.studypot.aistudyleader.llm.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.llm.admin.AdminLlmUsageFilter;
import com.studypot.aistudyleader.llm.admin.AdminLlmUsageRow;
import com.studypot.aistudyleader.llm.admin.AdminLlmUsageSummary;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsage;
import com.studypot.aistudyleader.llm.domain.LlmUsageAccessContext;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcLlmUsageRepository implements LlmUsageRepository {

	private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
	};

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	JdbcLlmUsageRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public boolean existsStudyGroup(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(LlmUsageJdbcSql.EXISTS_STUDY_GROUP, Boolean.class, uuid(groupId)));
	}

	@Override
	public Optional<LlmUsageAccessContext> findAccessContext(UUID groupId, UUID userId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		return queryOne(LlmUsageJdbcSql.SELECT_ACCESS_CONTEXT, this::mapAccessContext, uuid(groupId), uuid(userId));
	}

	@Override
	public boolean insertLlmUsage(LlmUsage usage) {
		Objects.requireNonNull(usage, "usage must not be null");
		return jdbcTemplate.update(
			LlmUsageJdbcSql.INSERT_LLM_USAGE,
			uuid(usage.id()),
			uuidOrNull(usage.userId()),
			uuidOrNull(usage.groupId()),
			usage.purpose().name(),
			usage.provider().name(),
			usage.model(),
			usage.inputTokens(),
			usage.outputTokens(),
			usage.totalCostUsd(),
			usage.latencyMs(),
			usage.status().name(),
			usage.errorCode(),
			json(usage.requestPayload(), "LLM usage request payload"),
			usage.responseSummary(),
			Date.valueOf(usage.createdDateUtc()),
			timestamp(usage.createdAt())
		) == 1;
	}

	@Override
	public List<LlmUsage> findGroupUsage(UUID groupId, int limit) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return jdbcTemplate.query(LlmUsageJdbcSql.SELECT_GROUP_USAGE, this::mapUsage, uuid(groupId), limit);
	}

	@Override
	public List<LlmUsage> findUserUsage(UUID userId, int limit) {
		Objects.requireNonNull(userId, "userId must not be null");
		return jdbcTemplate.query(LlmUsageJdbcSql.SELECT_USER_USAGE, this::mapUsage, uuid(userId), limit);
	}

	@Override
	public Optional<String> findUserEmail(UUID userId) {
		Objects.requireNonNull(userId, "userId must not be null");
		return queryOne(LlmUsageJdbcSql.SELECT_USER_EMAIL, (resultSet, rowNumber) -> resultSet.getString("email"), uuid(userId));
	}

	@Override
	public List<AdminLlmUsageRow> findAdminUsage(AdminLlmUsageFilter filter) {
		Objects.requireNonNull(filter, "filter must not be null");
		List<Object> args = new ArrayList<>();
		String sql = LlmUsageJdbcSql.SELECT_ADMIN_USAGE_PREFIX
			+ whereClause(filter, args)
			+ LlmUsageJdbcSql.SELECT_ADMIN_USAGE_SUFFIX;
		args.add(filter.limit());
		return jdbcTemplate.query(sql, this::mapAdminRow, args.toArray());
	}

	@Override
	public AdminLlmUsageSummary summarizeAdminUsage(AdminLlmUsageFilter filter) {
		Objects.requireNonNull(filter, "filter must not be null");
		List<Object> args = new ArrayList<>();
		String sql = LlmUsageJdbcSql.SELECT_ADMIN_SUMMARY_PREFIX + whereClause(filter, args);
		return queryOne(sql, this::mapAdminSummary, args.toArray()).orElse(AdminLlmUsageSummary.EMPTY);
	}

	/**
	 * 필터 조건을 {@code u.} 별칭 기준 WHERE 절로 만들고, 바인딩 인자를 {@code args}에 순서대로 채웁니다.
	 * 조건이 없으면 빈 문자열을 반환합니다.
	 */
	private String whereClause(AdminLlmUsageFilter filter, List<Object> args) {
		List<String> conditions = new ArrayList<>();
		if (filter.groupId() != null) {
			conditions.add("u.group_id = ?");
			args.add(uuid(filter.groupId()));
		}
		if (filter.userId() != null) {
			conditions.add("u.user_id = ?");
			args.add(uuid(filter.userId()));
		}
		if (filter.purpose() != null) {
			conditions.add("u.purpose = ?");
			args.add(filter.purpose().name());
		}
		if (filter.status() != null) {
			conditions.add("u.status = ?");
			args.add(filter.status().name());
		}
		if (filter.from() != null) {
			conditions.add("u.created_at >= ?");
			args.add(timestamp(filter.from()));
		}
		if (filter.to() != null) {
			conditions.add("u.created_at < ?");
			args.add(timestamp(filter.to()));
		}
		if (conditions.isEmpty()) {
			return "";
		}
		return "where " + String.join("\n  and ", conditions) + "\n";
	}

	private AdminLlmUsageRow mapAdminRow(ResultSet resultSet, int rowNumber) throws SQLException {
		return new AdminLlmUsageRow(
			requiredUuid(resultSet, "id"),
			uuid(resultSet.getBytes("user_id")),
			resultSet.getString("user_nickname"),
			resultSet.getString("user_email"),
			uuid(resultSet.getBytes("group_id")),
			resultSet.getString("group_name"),
			LlmUsagePurpose.valueOf(requiredString(resultSet, "purpose")),
			LlmProvider.valueOf(requiredString(resultSet, "provider")),
			requiredString(resultSet, "model"),
			resultSet.getInt("input_tokens"),
			resultSet.getInt("output_tokens"),
			resultSet.getBigDecimal("total_cost_usd"),
			resultSet.getObject("latency_ms", Integer.class),
			LlmUsageStatus.valueOf(requiredString(resultSet, "status")),
			resultSet.getString("error_code"),
			resultSet.getString("request_payload"),
			resultSet.getString("response_summary"),
			requiredInstant(resultSet, "created_at")
		);
	}

	private AdminLlmUsageSummary mapAdminSummary(ResultSet resultSet, int rowNumber) throws SQLException {
		BigDecimal cost = resultSet.getBigDecimal("total_cost_usd");
		return new AdminLlmUsageSummary(
			resultSet.getLong("total_calls"),
			resultSet.getLong("success_calls"),
			resultSet.getLong("failed_calls"),
			resultSet.getLong("input_tokens"),
			resultSet.getLong("output_tokens"),
			cost == null ? BigDecimal.ZERO : cost
		);
	}

	private LlmUsageAccessContext mapAccessContext(ResultSet resultSet, int rowNumber) throws SQLException {
		return new LlmUsageAccessContext(
			requiredUuid(resultSet, "group_id"),
			requiredUuid(resultSet, "member_id"),
			StudyGroupStatus.valueOf(requiredString(resultSet, "group_status")),
			GroupMemberPermission.valueOf(requiredString(resultSet, "permission")),
			GroupMemberStatus.valueOf(requiredString(resultSet, "member_status"))
		);
	}

	private LlmUsage mapUsage(ResultSet resultSet, int rowNumber) throws SQLException {
		return new LlmUsage(
			requiredUuid(resultSet, "id"),
			uuid(resultSet.getBytes("user_id")),
			uuid(resultSet.getBytes("group_id")),
			LlmUsagePurpose.valueOf(requiredString(resultSet, "purpose")),
			LlmProvider.valueOf(requiredString(resultSet, "provider")),
			requiredString(resultSet, "model"),
			resultSet.getInt("input_tokens"),
			resultSet.getInt("output_tokens"),
			resultSet.getBigDecimal("total_cost_usd"),
			resultSet.getObject("latency_ms", Integer.class),
			LlmUsageStatus.valueOf(requiredString(resultSet, "status")),
			resultSet.getString("error_code"),
			readNullableMap(resultSet.getString("request_payload"), "LLM usage request payload"),
			resultSet.getString("response_summary"),
			requiredDate(resultSet, "created_date_utc"),
			requiredInstant(resultSet, "created_at")
		);
	}

	private <T> Optional<T> queryOne(String sql, ThrowingRowMapper<T> mapper, Object... args) {
		List<T> results = jdbcTemplate.query(sql, (resultSet, rowNumber) -> mapper.map(resultSet, rowNumber), args);
		return results.stream().findFirst();
	}

	private String json(Object value, String fieldName) {
		try {
			return objectMapper.writeValueAsString(value == null ? Map.of() : value);
		} catch (JsonProcessingException exception) {
			throw new LlmUsagePersistenceException(fieldName + " could not be serialized.", exception);
		}
	}

	private Map<String, Object> readNullableMap(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			return Map.of();
		}
		try {
			return objectMapper.readValue(value, OBJECT_MAP);
		} catch (JsonProcessingException exception) {
			throw new LlmUsagePersistenceException(fieldName + " could not be deserialized.", exception);
		}
	}

	private static byte[] uuid(UUID uuid) {
		return UuidBinary.toBytes(uuid);
	}

	private static byte[] uuidOrNull(UUID uuid) {
		return uuid == null ? null : UuidBinary.toBytes(uuid);
	}

	private static UUID requiredUuid(ResultSet resultSet, String columnName) throws SQLException {
		byte[] bytes = resultSet.getBytes(columnName);
		if (bytes == null) {
			throw new SQLException(columnName + " must not be null.");
		}
		return UuidBinary.fromBytes(bytes);
	}

	private static UUID uuid(byte[] bytes) {
		return bytes == null ? null : UuidBinary.fromBytes(bytes);
	}

	private static String requiredString(ResultSet resultSet, String columnName) throws SQLException {
		String value = resultSet.getString(columnName);
		if (value == null || value.isBlank()) {
			throw new SQLException(columnName + " must not be blank.");
		}
		return value;
	}

	private static LocalDate requiredDate(ResultSet resultSet, String columnName) throws SQLException {
		Date date = resultSet.getDate(columnName);
		if (date == null) {
			throw new SQLException(columnName + " must not be null.");
		}
		return date.toLocalDate();
	}

	private static Instant requiredInstant(ResultSet resultSet, String columnName) throws SQLException {
		Timestamp timestamp = resultSet.getTimestamp(columnName);
		if (timestamp == null) {
			throw new SQLException(columnName + " must not be null.");
		}
		return timestamp.toInstant();
	}

	private static Timestamp timestamp(Instant instant) {
		return instant == null ? null : Timestamp.from(instant);
	}

	private interface ThrowingRowMapper<T> {

		T map(ResultSet resultSet, int rowNumber) throws SQLException;
	}
}
