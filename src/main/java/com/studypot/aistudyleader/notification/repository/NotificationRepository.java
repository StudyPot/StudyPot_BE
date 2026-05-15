package com.studypot.aistudyleader.notification.repository;

import com.studypot.aistudyleader.notification.domain.Notification;
import com.studypot.aistudyleader.notification.domain.NotificationAccessContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

	boolean existsStudyGroup(UUID groupId);

	Optional<NotificationAccessContext> findAccessContext(UUID groupId, UUID userId);

	Optional<Notification> findNotification(UUID notificationId);

	Optional<Notification> findNotificationByIdempotencyKey(String idempotencyKey);

	List<Notification> findMyNotifications(UUID userId, boolean unreadOnly, int limit);

	List<Notification> findGroupNotifications(UUID groupId, int limit);

	List<UUID> findActiveGroupRecipientUserIds(UUID groupId);

	/**
	 * Persists a notification that is already in its target state and returns the
	 * existing row when another row with the same idempotency key was already
	 * created.
	 */
	Notification saveNotification(Notification notification);

	/**
	 * Persists a notification whose status is {@code FAILED}. If the idempotency
	 * key already exists for a pending or failed row, the existing row is marked
	 * failed again and its retry count is incremented.
	 */
	Notification recordFailedNotification(Notification notification);

	/**
	 * Atomically retries only a currently failed notification and returns the
	 * reloaded row after the conditional update.
	 */
	Notification retryFailedNotification(UUID notificationId, Instant deliveredAt);

	boolean markNotificationRead(UUID notificationId, UUID recipientUserId, Instant readAt);

	int markAllDeliveredNotificationsRead(UUID recipientUserId, Instant readAt);
}
