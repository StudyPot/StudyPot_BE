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
		return queryOne(AiConversationJdbcSql.SELECT_WEEK_GROUP_ID, (resultSet, rowNumber) -> UuidBinary.fromBytes(resultSet.getBytes("group_id")), uuid(weekId));
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
			UuidBinary.fromBytes(resultSet.getBytes("group_id")),
			UuidBinary.fromBytes(resultSet.getBytes("member_id")),
			StudyGroupStatus.valueOf(resultSet.getString("group_status")),
			GroupMemberPermission.valueOf(resultSet.getString("permission")),
			GroupMemberStatus.valueOf(resultSet.getString("member_status"))
		);
	}

	private AiRetrospectiveReference mapRetrospectiveReference(ResultSet resultSet, int rowNumber) throws SQLException {
		return new AiRetrospectiveReference(
			UuidBinary.fromBytes(resultSet.getBytes("group_id")),
			UuidBinary.fromBytes(resultSet.getBytes("member_id")),
			UuidBinary.fromBytes(resultSet.getBytes("curriculum_week_id"))
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

	private interface ThrowingRowMapper<T> {

		T map(ResultSet resultSet, int rowNumber) throws SQLException;
	}
}
