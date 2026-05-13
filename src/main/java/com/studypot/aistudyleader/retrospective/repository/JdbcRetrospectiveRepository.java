package com.studypot.aistudyleader.retrospective.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgressStatus;
import com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.retrospective.domain.Retrospective;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcRetrospectiveRepository implements RetrospectiveRepository {

	private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
	};

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

	private interface ThrowingRowMapper<T> {

		T map(ResultSet resultSet, int rowNumber) throws SQLException;
	}
}
