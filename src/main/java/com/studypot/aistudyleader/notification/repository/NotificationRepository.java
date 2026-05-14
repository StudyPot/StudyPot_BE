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

	Notification saveNotification(Notification notification);

	Notification recordFailedNotification(Notification notification);

	Notification retryFailedNotification(UUID notificationId, Instant deliveredAt);

	boolean markNotificationRead(UUID notificationId, UUID recipientUserId, Instant readAt);

	int markAllDeliveredNotificationsRead(UUID recipientUserId, Instant readAt);
}
