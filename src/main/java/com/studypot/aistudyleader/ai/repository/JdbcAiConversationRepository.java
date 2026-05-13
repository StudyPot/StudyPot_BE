package com.studypot.aistudyleader.ai.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.ai.domain.AiConversation;
import com.studypot.aistudyleader.ai.domain.AiConversationMembershipContext;
import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageContext;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageCursor;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageSenderType;
import com.studypot.aistudyleader.ai.domain.AiConversationStatus;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcAiConversationRepository implements AiConversationRepository {

	private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
	};

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	JdbcAiConversationRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
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

	@Override
	public boolean existsConversation(UUID conversationId) {
		Objects.requireNonNull(conversationId, "conversationId must not be null");
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
			AiConversationJdbcSql.EXISTS_CONVERSATION,
			Boolean.class,
			uuid(conversationId)
		));
	}

	@Override
	public Optional<AiConversationMessageContext> findMessageContext(UUID conversationId, UUID userId) {
		Objects.requireNonNull(conversationId, "conversationId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		return queryOne(AiConversationJdbcSql.SELECT_MESSAGE_CONTEXT, this::mapMessageContext, uuid(conversationId), uuid(userId));
	}

	@Override
	public boolean insertMessage(AiConversationMessage message) {
		Objects.requireNonNull(message, "message must not be null");
		return jdbcTemplate.update(
			AiConversationJdbcSql.INSERT_MESSAGE,
			uuid(message.id()),
			uuid(message.conversationId()),
			uuidOrNull(message.llmUsageId()),
			message.senderType().name(),
			message.content(),
			json(message.metadata(), "AI conversation message metadata"),
			timestamp(message.createdAt())
		) == 1;
	}

	@Override
	public List<AiConversationMessage> findMessages(UUID conversationId, AiConversationMessageCursor cursor, int limit) {
		Objects.requireNonNull(conversationId, "conversationId must not be null");
		Timestamp cursorCreatedAt = cursor == null ? null : timestamp(cursor.createdAt());
		byte[] cursorId = cursor == null ? null : uuid(cursor.id());
		return jdbcTemplate.query(
			AiConversationJdbcSql.SELECT_MESSAGES,
			this::mapMessage,
			uuid(conversationId),
			cursorCreatedAt,
			cursorCreatedAt,
			cursorCreatedAt,
			cursorId,
			limit
		);
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

	private AiConversationMessageContext mapMessageContext(ResultSet resultSet, int rowNumber) throws SQLException {
		return new AiConversationMessageContext(
			requiredUuid(resultSet, "conversation_id"),
			requiredUuid(resultSet, "group_id"),
			requiredUuid(resultSet, "member_id"),
			AiConversationStatus.valueOf(requiredString(resultSet, "conversation_status")),
			StudyGroupStatus.valueOf(requiredString(resultSet, "group_status")),
			GroupMemberStatus.valueOf(requiredString(resultSet, "member_status"))
		);
	}

	private AiConversationMessage mapMessage(ResultSet resultSet, int rowNumber) throws SQLException {
		return new AiConversationMessage(
			requiredUuid(resultSet, "id"),
			requiredUuid(resultSet, "conversation_id"),
			uuid(resultSet.getBytes("llm_usage_id")),
			AiConversationMessageSenderType.valueOf(requiredString(resultSet, "sender_type")),
			requiredString(resultSet, "content"),
			readNullableMap(resultSet.getString("metadata"), "AI conversation message metadata"),
			requiredInstant(resultSet, "created_at")
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

	private String json(Object value, String fieldName) {
		try {
			return objectMapper.writeValueAsString(value == null ? Map.of() : value);
		} catch (JsonProcessingException exception) {
			throw new AiConversationPersistenceException(fieldName + " could not be serialized.", exception);
		}
	}

	private Map<String, Object> readNullableMap(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			return Map.of();
		}
		try {
			return objectMapper.readValue(value, OBJECT_MAP);
		} catch (JsonProcessingException exception) {
			throw new AiConversationPersistenceException(fieldName + " could not be deserialized.", exception);
		}
	}

	private static Instant requiredInstant(ResultSet resultSet, String columnName) throws SQLException {
		Timestamp timestamp = resultSet.getTimestamp(columnName);
		if (timestamp == null) {
			throw new SQLException(columnName + " must not be null.");
		}
		return timestamp.toInstant();
	}

	private interface ThrowingRowMapper<T> {

		T map(ResultSet resultSet, int rowNumber) throws SQLException;
	}
}
