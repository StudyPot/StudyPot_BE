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
import com.studypot.aistudyleader.ai.domain.AiConversationPromptContext;
import com.studypot.aistudyleader.ai.domain.AiConversationStatus;
import com.studypot.aistudyleader.ai.domain.AiConversationType;
import com.studypot.aistudyleader.ai.domain.AiRetrospectiveReference;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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
	public Optional<AiConversation> findOpenTeamLeadConversation(UUID groupId, UUID memberId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		return queryOne(AiConversationJdbcSql.SELECT_OPEN_TEAM_LEAD_CONVERSATION, this::mapConversation, uuid(groupId), uuid(memberId));
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

	@Override
	public AiConversationPromptContext findPromptContext(AiConversationMessageContext context, int recentMessageLimit) {
		Objects.requireNonNull(context, "context must not be null");
		int limit = Math.max(1, recentMessageLimit);
		return new AiConversationPromptContext(
			conversationContext(context),
			findRecentMessagesForPrompt(context.conversationId(), limit),
			context.curriculumWeekId() == null
				? Map.of("status", "NOT_AVAILABLE")
				: queryOne(AiConversationJdbcSql.SELECT_WEEK_PROMPT_CONTEXT, this::mapWeekPromptContext, uuid(context.curriculumWeekId()))
					.orElse(Map.of("status", "NOT_AVAILABLE")),
			context.curriculumWeekId() == null
				? List.of()
				: jdbcTemplate.query(
					AiConversationJdbcSql.SELECT_TASK_PROMPT_CONTEXT,
					this::mapTaskPromptContext,
					uuid(context.memberId()),
					uuid(context.curriculumWeekId())
				),
			context.curriculumWeekId() == null
				? Map.of("status", "NOT_AVAILABLE")
				: queryOne(
					AiConversationJdbcSql.SELECT_PROGRESS_PROMPT_CONTEXT,
					this::mapProgressPromptContext,
					uuid(context.curriculumWeekId()),
					uuid(context.memberId())
				).orElse(Map.of("status", "NOT_AVAILABLE")),
			context.retrospectiveId() == null
				? Map.of("status", "NOT_AVAILABLE")
				: queryOne(
					AiConversationJdbcSql.SELECT_RETROSPECTIVE_PROMPT_CONTEXT,
					this::mapRetrospectivePromptContext,
					uuid(context.retrospectiveId()),
					uuid(context.memberId())
				).orElse(Map.of("status", "NOT_AVAILABLE"))
		);
	}

	@Override
	public boolean updateConversationSummary(UUID conversationId, String summary, Instant updatedAt) {
		Objects.requireNonNull(conversationId, "conversationId must not be null");
		if (summary == null || summary.isBlank()) {
			throw new IllegalArgumentException("summary must not be blank");
		}
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		return jdbcTemplate.update(
			AiConversationJdbcSql.UPDATE_CONVERSATION_SUMMARY,
			summary.strip(),
			timestamp(updatedAt),
			uuid(conversationId)
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

	private AiConversation mapConversation(ResultSet resultSet, int rowNumber) throws SQLException {
		return new AiConversation(
			requiredUuid(resultSet, "id"),
			requiredUuid(resultSet, "group_id"),
			requiredUuid(resultSet, "member_id"),
			uuid(resultSet.getBytes("curriculum_week_id")),
			uuid(resultSet.getBytes("retrospective_id")),
			AiConversationType.valueOf(requiredString(resultSet, "conversation_type")),
			AiConversationStatus.valueOf(requiredString(resultSet, "status")),
			resultSet.getString("summary"),
			requiredInstant(resultSet, "opened_at"),
			instant(resultSet.getTimestamp("closed_at")),
			requiredInstant(resultSet, "created_at"),
			requiredInstant(resultSet, "updated_at")
		);
	}

	private AiConversationMessageContext mapMessageContext(ResultSet resultSet, int rowNumber) throws SQLException {
		return new AiConversationMessageContext(
			requiredUuid(resultSet, "conversation_id"),
			requiredUuid(resultSet, "group_id"),
			requiredUuid(resultSet, "member_id"),
			uuid(resultSet.getBytes("curriculum_week_id")),
			uuid(resultSet.getBytes("retrospective_id")),
			AiConversationType.valueOf(requiredString(resultSet, "conversation_type")),
			resultSet.getString("summary"),
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

	private List<Map<String, Object>> findRecentMessagesForPrompt(UUID conversationId, int limit) {
		List<AiConversationMessage> newestFirst = jdbcTemplate.query(
			AiConversationJdbcSql.SELECT_RECENT_MESSAGES_FOR_PROMPT,
			this::mapMessage,
			uuid(conversationId),
			limit
		);
		List<AiConversationMessage> chronological = new ArrayList<>(newestFirst);
		Collections.reverse(chronological);
		return chronological.stream()
			.map(this::messageContext)
			.toList();
	}

	private Map<String, Object> conversationContext(AiConversationMessageContext context) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", context.conversationId().toString());
		result.put("groupId", context.groupId().toString());
		result.put("memberId", context.memberId().toString());
		putUuid(result, "weekId", context.curriculumWeekId());
		putUuid(result, "retrospectiveId", context.retrospectiveId());
		result.put("conversationType", context.conversationType().name());
		result.put("status", context.conversationStatus().name());
		if (context.summary() != null && !context.summary().isBlank()) {
			result.put("summary", context.summary());
		}
		return result;
	}

	private Map<String, Object> messageContext(AiConversationMessage message) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", message.id().toString());
		result.put("senderType", message.senderType().name());
		result.put("content", message.content());
		result.put("metadata", message.metadata());
		putUuid(result, "llmUsageId", message.llmUsageId());
		result.put("createdAt", message.createdAt().toString());
		return result;
	}

	private Map<String, Object> mapWeekPromptContext(ResultSet resultSet, int rowNumber) throws SQLException {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("status", "AVAILABLE");
		result.put("id", requiredUuid(resultSet, "id").toString());
		result.put("weekNumber", resultSet.getInt("week_number"));
		result.put("title", requiredString(resultSet, "title"));
		putText(result, "description", resultSet.getString("description"));
		putText(result, "sprintGoal", resultSet.getString("sprint_goal"));
		putJson(result, "learningGoals", resultSet.getString("learning_goals"), "week learning goals");
		putJson(result, "resources", resultSet.getString("resources"), "week resources");
		result.put("weekStatus", requiredString(resultSet, "status"));
		putInstant(result, "startsAt", requiredInstant(resultSet, "starts_at"));
		putInstant(result, "endsAt", requiredInstant(resultSet, "ends_at"));
		return result;
	}

	private Map<String, Object> mapTaskPromptContext(ResultSet resultSet, int rowNumber) throws SQLException {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", requiredUuid(resultSet, "id").toString());
		result.put("displayOrder", resultSet.getInt("display_order"));
		result.put("taskType", requiredString(resultSet, "task_type"));
		result.put("title", requiredString(resultSet, "title"));
		putText(result, "description", resultSet.getString("description"));
		result.put("required", resultSet.getBoolean("required"));
		putInstant(result, "dueAt", instant(resultSet.getTimestamp("due_at")));
		result.put("completionStatus", requiredString(resultSet, "completion_status"));
		putInstant(result, "completedAt", instant(resultSet.getTimestamp("completed_at")));
		putInstant(result, "reasonSubmittedAt", instant(resultSet.getTimestamp("reason_submitted_at")));
		putText(result, "completionNote", resultSet.getString("completion_note"));
		putText(result, "incompleteReason", resultSet.getString("incomplete_reason"));
		return result;
	}

	private Map<String, Object> mapProgressPromptContext(ResultSet resultSet, int rowNumber) throws SQLException {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("status", "AVAILABLE");
		result.put("id", requiredUuid(resultSet, "id").toString());
		result.put("progressStatus", requiredString(resultSet, "status"));
		putInstant(result, "startedAt", instant(resultSet.getTimestamp("started_at")));
		putInstant(result, "dueAt", instant(resultSet.getTimestamp("due_at")));
		putInstant(result, "completedAt", instant(resultSet.getTimestamp("completed_at")));
		putInstant(result, "reasonSubmittedAt", instant(resultSet.getTimestamp("reason_submitted_at")));
		putText(result, "completionNote", resultSet.getString("completion_note"));
		putText(result, "incompleteReason", resultSet.getString("incomplete_reason"));
		return result;
	}

	private Map<String, Object> mapRetrospectivePromptContext(ResultSet resultSet, int rowNumber) throws SQLException {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("status", "AVAILABLE");
		result.put("id", requiredUuid(resultSet, "id").toString());
		result.put("retrospectiveStatus", requiredString(resultSet, "status"));
		result.put("aiFeedback", readNullableMap(resultSet.getString("ai_feedback"), "retrospective AI feedback"));
		result.put(
			"nextWeekAdjustment",
			readNullableMap(resultSet.getString("next_week_adjustment"), "retrospective next week adjustment")
		);
		putInstant(result, "requestedAt", instant(resultSet.getTimestamp("requested_at")));
		putInstant(result, "completedAt", instant(resultSet.getTimestamp("completed_at")));
		return result;
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

	private Object readNullableJson(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readValue(value, Object.class);
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

	private static Instant instant(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toInstant();
	}

	private static void putText(Map<String, Object> target, String key, String value) {
		if (value != null && !value.isBlank()) {
			target.put(key, value.strip());
		}
	}

	private static void putUuid(Map<String, Object> target, String key, UUID value) {
		if (value != null) {
			target.put(key, value.toString());
		}
	}

	private static void putInstant(Map<String, Object> target, String key, Instant value) {
		if (value != null) {
			target.put(key, value.toString());
		}
	}

	private void putJson(Map<String, Object> target, String key, String value, String fieldName) {
		Object jsonValue = readNullableJson(value, fieldName);
		if (jsonValue != null) {
			target.put(key, jsonValue);
		}
	}

	private interface ThrowingRowMapper<T> {

		T map(ResultSet resultSet, int rowNumber) throws SQLException;
	}
}
