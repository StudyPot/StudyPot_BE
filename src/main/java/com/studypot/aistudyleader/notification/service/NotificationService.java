package com.studypot.aistudyleader.notification.service;

import com.studypot.aistudyleader.notification.domain.Notification;
import com.studypot.aistudyleader.notification.domain.NotificationAccessContext;
import com.studypot.aistudyleader.notification.domain.NotificationStatus;
import com.studypot.aistudyleader.notification.repository.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class NotificationService {

	private static final int DEFAULT_NOTIFICATION_LIMIT = 100;

	private final NotificationRepository repository;
	private final Clock clock;

	public NotificationService(NotificationRepository repository, Clock clock) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	@Transactional(readOnly = true)
	public List<Notification> listMyNotifications(ListMyNotificationsQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		return repository.findMyNotifications(query.authenticatedUserId(), query.unreadOnly(), DEFAULT_NOTIFICATION_LIMIT);
	}

	@Transactional
	public Notification markNotificationRead(MarkNotificationReadCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		Notification notification = repository.findNotification(command.notificationId())
			.orElseThrow(() -> new NotificationNotFoundException("notification was not found."));
		if (!notification.belongsToRecipient(command.authenticatedUserId())) {
			throw new NotificationAccessDeniedException("authenticated user is not the notification recipient.");
		}
		if (notification.status() == NotificationStatus.READ) {
			return notification;
		}
		if (!notification.status().canBeMarkedRead()) {
			throw new NotificationMutationRejectedException("only delivered notifications can be marked read.");
		}
		Instant now = clock.instant();
		if (!repository.markNotificationRead(notification.id(), command.authenticatedUserId(), now)) {
			throw new NotificationMutationRejectedException("notification read status could not be updated.");
		}
		return notification.markRead(now);
	}

	@Transactional
	public void markAllMyNotificationsRead(MarkAllMyNotificationsReadCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		repository.markAllDeliveredNotificationsRead(command.authenticatedUserId(), clock.instant());
	}

	@Transactional(readOnly = true)
	public List<Notification> listGroupNotifications(ListGroupNotificationsQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		NotificationAccessContext context = requireAccessContext(query.groupId(), query.authenticatedUserId());
		if (!context.canReadGroupNotificationLogs()) {
			throw new NotificationAccessDeniedException("only the study group owner can read notification logs.");
		}
		return repository.findGroupNotifications(query.groupId(), DEFAULT_NOTIFICATION_LIMIT);
	}

	private NotificationAccessContext requireAccessContext(UUID groupId, UUID userId) {
		return repository.findAccessContext(groupId, userId)
			.orElseGet(() -> {
				if (!repository.existsStudyGroup(groupId)) {
					throw new NotificationGroupNotFoundException("study group was not found.");
				}
				throw new NotificationAccessDeniedException("authenticated user is not an owner of this study group.");
			});
	}
}
