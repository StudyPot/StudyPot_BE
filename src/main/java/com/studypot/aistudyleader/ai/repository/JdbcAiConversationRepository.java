package com.studypot.aistudyleader.ai.repository;

import com.studypot.aistudyleader.ai.domain.AiConversation;
import com.studypot.aistudyleader.ai.domain.AiConversationMembershipContext;
import com.studypot.aistudyleader.ai.domain.AiRetrospectiveReference;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcAiConversationRepository implements AiConversationRepository {

	private final JdbcTemplate jdbcTemplate;

	JdbcAiConversationRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
	}

	@Override
	public boolean existsStudyGroup(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(AiConversationJdbcSql.EXISTS_STUDY_GROUP, Boolean.class, uuid(groupId)));
	}

	@Override
	public Optional<AiConversationMembershipContext> findMembership(UUID groupId, UUID userId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		return queryOne(AiConversationJdbcSql.SELECT_MEMBERSHIP, this::mapMembership, uuid(groupId), uuid(userId));
	}

	@Override
	public Optional<UUID> findWeekGroupId(UUID weekId) {
		Objects.requireNonNull(weekId, "weekId must not be null");
		return queryOne(AiConversationJdbcSql.SELECT_WEEK_GROUP_ID, (resultSet, rowNumber) -> requiredUuid(resultSet, "group_id"), uuid(weekId));
	}

	@Override
	public Optional<AiRetrospectiveReference> findRetrospectiveReference(UUID retrospectiveId) {
		Objects.requireNonNull(retrospectiveId, "retrospectiveId must not be null");
		return queryOne(AiConversationJdbcSql.SELECT_RETROSPECTIVE_REFERENCE, this::mapRetrospectiveReference, uuid(retrospectiveId));
	}

	@Override
	public boolean insertConversation(AiConversation conversation) {
		Objects.requireNonNull(conversation, "conversation must not be null");
		return jdbcTemplate.update(
			AiConversationJdbcSql.INSERT_CONVERSATION,
			uuid(conversation.id()),
			uuid(conversation.groupId()),
			uuid(conversation.memberId()),
			uuidOrNull(conversation.curriculumWeekId()),
			uuidOrNull(conversation.retrospectiveId()),
			conversation.conversationType().name(),
			conversation.status().name(),
			conversation.summary(),
			timestamp(conversation.openedAt()),
			timestamp(conversation.closedAt()),
			timestamp(conversation.createdAt()),
			timestamp(conversation.updatedAt())
		) == 1;
	}

	private AiConversationMembershipContext mapMembership(ResultSet resultSet, int rowNumber) throws SQLException {
		return new AiConversationMembershipContext(
			requiredUuid(resultSet, "group_id"),
			requiredUuid(resultSet, "member_id"),
			StudyGroupStatus.valueOf(requiredString(resultSet, "group_status")),
			GroupMemberPermission.valueOf(requiredString(resultSet, "permission")),
			GroupMemberStatus.valueOf(requiredString(resultSet, "member_status"))
		);
	}

	private AiRetrospectiveReference mapRetrospectiveReference(ResultSet resultSet, int rowNumber) throws SQLException {
		return new AiRetrospectiveReference(
			requiredUuid(resultSet, "group_id"),
			requiredUuid(resultSet, "member_id"),
			requiredUuid(resultSet, "curriculum_week_id")
		);
	}

	private <T> Optional<T> queryOne(String sql, ThrowingRowMapper<T> mapper, Object... args) {
		List<T> results = jdbcTemplate.query(sql, (resultSet, rowNumber) -> mapper.map(resultSet, rowNumber), args);
		return results.stream().findFirst();
	}

	private static byte[] uuid(UUID uuid) {
		return UuidBinary.toBytes(uuid);
	}

	private static byte[] uuidOrNull(UUID uuid) {
		return uuid == null ? null : UuidBinary.toBytes(uuid);
	}

	private static Timestamp timestamp(Instant instant) {
		return instant == null ? null : Timestamp.from(instant);
	}

	private static UUID requiredUuid(ResultSet resultSet, String columnName) throws SQLException {
		byte[] bytes = resultSet.getBytes(columnName);
		if (bytes == null) {
			throw new SQLException(columnName + " must not be null.");
		}
		return UuidBinary.fromBytes(bytes);
	}

	private static String requiredString(ResultSet resultSet, String columnName) throws SQLException {
		String value = resultSet.getString(columnName);
		if (value == null || value.isBlank()) {
			throw new SQLException(columnName + " must not be blank.");
		}
		return value;
	}

	private interface ThrowingRowMapper<T> {

		T map(ResultSet resultSet, int rowNumber) throws SQLException;
	}
}
