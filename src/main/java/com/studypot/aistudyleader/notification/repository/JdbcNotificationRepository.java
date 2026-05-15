package com.studypot.aistudyleader.notification.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.notification.domain.Notification;
import com.studypot.aistudyleader.notification.domain.NotificationAccessContext;
import com.studypot.aistudyleader.notification.domain.NotificationChannel;
import com.studypot.aistudyleader.notification.domain.NotificationRelatedResources;
import com.studypot.aistudyleader.notification.domain.NotificationStatus;
import com.studypot.aistudyleader.notification.domain.NotificationType;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcNotificationRepository implements NotificationRepository {

	private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
	};
	private static final int MAX_NOTIFICATION_LIMIT = 1000;

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	JdbcNotificationRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public boolean existsStudyGroup(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(NotificationJdbcSql.EXISTS_STUDY_GROUP, Boolean.class, uuid(groupId)));
	}

	@Override
	public Optional<NotificationAccessContext> findAccessContext(UUID groupId, UUID userId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		return queryOne(NotificationJdbcSql.SELECT_ACCESS_CONTEXT, this::mapAccessContext, uuid(groupId), uuid(userId));
	}

	@Override
	public Optional<Notification> findNotification(UUID notificationId) {
		Objects.requireNonNull(notificationId, "notificationId must not be null");
		return queryOne(NotificationJdbcSql.SELECT_NOTIFICATION_BY_ID, this::mapNotification, uuid(notificationId));
	}

	@Override
	public Optional<Notification> findNotificationByIdempotencyKey(String idempotencyKey) {
		String key = requireText(idempotencyKey, "idempotencyKey");
		return queryOne(NotificationJdbcSql.SELECT_NOTIFICATION_BY_IDEMPOTENCY_KEY, this::mapNotification, key);
	}

	@Override
	public List<Notification> findMyNotifications(UUID userId, boolean unreadOnly, int limit) {
		Objects.requireNonNull(userId, "userId must not be null");
		String sql = unreadOnly ? NotificationJdbcSql.SELECT_MY_UNREAD_NOTIFICATIONS : NotificationJdbcSql.SELECT_MY_NOTIFICATIONS;
		return jdbcTemplate.query(sql, this::mapNotification, uuid(userId), clampedLimit(limit));
	}

	@Override
	public List<Notification> findGroupNotifications(UUID groupId, int limit) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return jdbcTemplate.query(NotificationJdbcSql.SELECT_GROUP_NOTIFICATIONS, this::mapNotification, uuid(groupId), clampedLimit(limit));
	}

	@Override
	public List<UUID> findActiveGroupRecipientUserIds(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return jdbcTemplate.query(
			NotificationJdbcSql.SELECT_ACTIVE_GROUP_RECIPIENT_USER_IDS,
			(resultSet, rowNumber) -> requiredUuid(resultSet, "user_id"),
			uuid(groupId)
		);
	}

	@Override
	public Notification saveNotification(Notification notification) {
		Objects.requireNonNull(notification, "notification must not be null");
		try {
			insertNotification(notification);
		} catch (DuplicateKeyException exception) {
			return findNotificationByIdempotencyKey(notification.idempotencyKey())
				.orElseThrow(() -> new NotificationPersistenceException("duplicate notification could not be loaded.", exception));
		}
		return notification;
	}

	@Override
	public Notification recordFailedNotification(Notification notification) {
		Objects.requireNonNull(notification, "notification must not be null");
		try {
			insertNotification(notification);
			return notification;
		} catch (DuplicateKeyException exception) {
			jdbcTemplate.update(
				NotificationJdbcSql.MARK_NOTIFICATION_FAILED_BY_IDEMPOTENCY_KEY,
				notification.errorMessage(),
				notification.idempotencyKey()
			);
			return findNotificationByIdempotencyKey(notification.idempotencyKey())
				.orElseThrow(() -> new NotificationPersistenceException("failed notification could not be loaded.", exception));
		}
	}

	@Override
	public Notification retryFailedNotification(UUID notificationId, Instant deliveredAt) {
		Objects.requireNonNull(notificationId, "notificationId must not be null");
		Objects.requireNonNull(deliveredAt, "deliveredAt must not be null");
		int updated = jdbcTemplate.update(
			NotificationJdbcSql.RETRY_FAILED_NOTIFICATION,
			timestamp(deliveredAt),
			uuid(notificationId)
		);
		if (updated != 1) {
			throw new NotificationPersistenceException("failed notification could not be retried.");
		}
		return findNotification(notificationId)
			.orElseThrow(() -> new NotificationPersistenceException("retried notification could not be loaded."));
	}

	@Override
	public boolean markNotificationRead(UUID notificationId, UUID recipientUserId, Instant readAt) {
		Objects.requireNonNull(notificationId, "notificationId must not be null");
		Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
		Objects.requireNonNull(readAt, "readAt must not be null");
		return jdbcTemplate.update(
			NotificationJdbcSql.MARK_NOTIFICATION_READ,
			timestamp(readAt),
			uuid(notificationId),
			uuid(recipientUserId)
		) == 1;
	}

	@Override
	public int markAllDeliveredNotificationsRead(UUID recipientUserId, Instant readAt) {
		Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
		Objects.requireNonNull(readAt, "readAt must not be null");
		return jdbcTemplate.update(NotificationJdbcSql.MARK_ALL_DELIVERED_NOTIFICATIONS_READ, timestamp(readAt), uuid(recipientUserId));
	}

	private void insertNotification(Notification notification) {
		jdbcTemplate.update(
			NotificationJdbcSql.INSERT_NOTIFICATION,
			uuid(notification.id()),
			uuid(notification.groupId()),
			uuid(notification.recipientUserId()),
			uuidOrNull(notification.relatedResources().onboardingResponseId()),
			uuidOrNull(notification.relatedResources().weekId()),
			uuidOrNull(notification.relatedResources().taskCompletionId()),
			uuidOrNull(notification.relatedResources().retrospectiveId()),
			notification.notificationType().name(),
			notification.channel().name(),
			notification.idempotencyKey(),
			notification.title(),
			notification.body(),
			json(notification.payload(), "notification payload"),
			notification.status().name(),
			timestamp(notification.deliveredAt()),
			timestamp(notification.readAt()),
			notification.errorMessage(),
			notification.retryCount(),
			timestamp(notification.scheduledAt()),
			timestamp(notification.createdAt())
		);
	}

	private NotificationAccessContext mapAccessContext(ResultSet resultSet, int rowNumber) throws SQLException {
		return new NotificationAccessContext(
			requiredUuid(resultSet, "group_id"),
			requiredUuid(resultSet, "member_id"),
			requiredEnum(resultSet, "group_status", StudyGroupStatus.class),
			requiredEnum(resultSet, "permission", GroupMemberPermission.class),
			requiredEnum(resultSet, "member_status", GroupMemberStatus.class)
		);
	}

	private Notification mapNotification(ResultSet resultSet, int rowNumber) throws SQLException {
		return new Notification(
			requiredUuid(resultSet, "id"),
			requiredUuid(resultSet, "group_id"),
			requiredUuid(resultSet, "recipient_user_id"),
			new NotificationRelatedResources(
				uuid(resultSet.getBytes("related_onboarding_response_id")),
				uuid(resultSet.getBytes("related_week_id")),
				uuid(resultSet.getBytes("related_task_completion_id")),
				uuid(resultSet.getBytes("related_retrospective_id"))
			),
			requiredEnum(resultSet, "notification_type", NotificationType.class),
			requiredEnum(resultSet, "channel", NotificationChannel.class),
			requiredString(resultSet, "idempotency_key"),
			requiredString(resultSet, "title"),
			resultSet.getString("body"),
			readNullableMap(resultSet.getString("payload"), "notification payload"),
			requiredEnum(resultSet, "status", NotificationStatus.class),
			instant(resultSet.getTimestamp("delivered_at")),
			instant(resultSet.getTimestamp("read_at")),
			resultSet.getString("error_message"),
			resultSet.getInt("retry_count"),
			instant(resultSet.getTimestamp("scheduled_at")),
			requiredInstant(resultSet, "created_at")
		);
	}

	private <T> Optional<T> queryOne(String sql, ThrowingRowMapper<T> mapper, Object... args) {
		List<T> results = jdbcTemplate.query(sql, (resultSet, rowNumber) -> mapper.map(resultSet, rowNumber), args);
		return results.stream().findFirst();
	}

	private Map<String, Object> readNullableMap(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			return Map.of();
		}
		try {
			return objectMapper.readValue(value, OBJECT_MAP);
		} catch (JsonProcessingException exception) {
			throw new NotificationPersistenceException(fieldName + " could not be deserialized.", exception);
		}
	}

	private String json(Map<String, Object> value, String fieldName) {
		try {
			return objectMapper.writeValueAsString(value == null ? Map.of() : value);
		} catch (JsonProcessingException exception) {
			throw new NotificationPersistenceException(fieldName + " could not be serialized.", exception);
		}
	}

	private static byte[] uuid(UUID uuid) {
		return UuidBinary.toBytes(uuid);
	}

	private static byte[] uuidOrNull(UUID uuid) {
		return uuid == null ? null : UuidBinary.toBytes(uuid);
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank.");
		}
		return value.strip();
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

	private static Timestamp timestamp(Instant instant) {
		return instant == null ? null : Timestamp.from(instant);
	}

	private static int clampedLimit(int limit) {
		return Math.min(Math.max(1, limit), MAX_NOTIFICATION_LIMIT);
	}

	private static <E extends Enum<E>> E requiredEnum(ResultSet resultSet, String columnName, Class<E> enumType)
		throws SQLException {
		String value = requiredString(resultSet, columnName);
		try {
			return Enum.valueOf(enumType, value);
		} catch (IllegalArgumentException exception) {
			throw new SQLException(columnName + " has invalid value: " + value, exception);
		}
	}

	private interface ThrowingRowMapper<T> {

		T map(ResultSet resultSet, int rowNumber) throws SQLException;
	}
}
