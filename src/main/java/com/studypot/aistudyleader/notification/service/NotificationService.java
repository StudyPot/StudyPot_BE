package com.studypot.aistudyleader.notification.service;

import com.studypot.aistudyleader.notification.domain.Notification;
import com.studypot.aistudyleader.notification.domain.NotificationAccessContext;
import com.studypot.aistudyleader.notification.domain.NotificationStatus;
import com.studypot.aistudyleader.notification.domain.NotificationType;
import com.studypot.aistudyleader.notification.repository.NotificationRepository;
import com.studypot.aistudyleader.global.domain.UuidV7;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class NotificationService implements NotificationEventPublisher {

	private static final int DEFAULT_NOTIFICATION_LIMIT = 100;
	private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
	private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

	private final NotificationRepository repository;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;

	public NotificationService(NotificationRepository repository, Clock clock) {
		this(repository, clock, UuidV7::generate);
	}

	public NotificationService(NotificationRepository repository, Clock clock, Supplier<UUID> idGenerator) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
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

	@Transactional
	public Notification createNotification(CreateNotificationCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		return repository.saveNotification(command.toDeliveredNotification(idGenerator.get(), clock.instant()));
	}

	@Transactional
	public Notification recordNotificationFailure(RecordNotificationFailureCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		Instant now = clock.instant();
		return repository.recordFailedNotification(
			command.notification().toFailedNotification(idGenerator.get(), redactError(command.errorMessage()), now)
		);
	}

	@Transactional
	public Notification retryNotification(RetryNotificationCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		Notification notification = repository.findNotification(command.notificationId())
			.orElseThrow(() -> new NotificationNotFoundException("notification was not found."));
		if (notification.status() != NotificationStatus.FAILED) {
			throw new NotificationMutationRejectedException("only failed notifications can be retried.");
		}
		return repository.retryFailedNotification(command.notificationId(), clock.instant());
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

	@Override
	public void publishOnboardingRequested(UUID groupId, UUID recipientUserId) {
		publishAfterCommit(NotificationCommandFactory.onboardingRequested(groupId, recipientUserId));
	}

	@Override
	public void publishWeekStarted(UUID groupId, UUID weekId, int weekNumber, String weekTitle) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(weekId, "weekId must not be null");
		String safeWeekTitle = requireText(weekTitle, "weekTitle");
		publishAfterCommit(() -> {
			List<UUID> recipients = repository.findActiveGroupRecipientUserIds(groupId);
			for (UUID recipientUserId : recipients) {
				createNotificationSafely(NotificationCommandFactory.weekStarted(groupId, recipientUserId, weekId, weekNumber, safeWeekTitle));
			}
		});
	}

	@Override
	public void publishTaskDueReminder(
		UUID groupId,
		UUID recipientUserId,
		UUID weekId,
		UUID taskCompletionId,
		String taskTitle,
		Instant dueAt
	) {
		publishAfterCommit(NotificationCommandFactory.taskNotification(
			NotificationType.TASK_DUE_REMINDER,
			groupId,
			recipientUserId,
			weekId,
			taskCompletionId,
			taskTitle,
			"마감이 가까운 todo가 있어요",
			"마감 전에 todo를 완료해 주세요.",
			Map.of("dueAt", Objects.requireNonNull(dueAt, "dueAt must not be null").toString())
		));
	}

	@Override
	public void publishTaskOverdueCheck(UUID groupId, UUID recipientUserId, UUID taskCompletionId, String taskTitle) {
		publishAfterCommit(NotificationCommandFactory.taskNotification(
			NotificationType.TASK_OVERDUE_CHECK,
			groupId,
			recipientUserId,
			null,
			taskCompletionId,
			taskTitle,
			"마감이 지난 todo가 있어요",
			"마감이 지난 todo 상태를 확인해 주세요.",
			Map.of()
		));
	}

	@Override
	public void publishIncompleteReasonRequested(UUID groupId, UUID recipientUserId, UUID taskCompletionId, String taskTitle) {
		publishAfterCommit(NotificationCommandFactory.taskNotification(
			NotificationType.INCOMPLETE_REASON_REQUESTED,
			groupId,
			recipientUserId,
			null,
			taskCompletionId,
			taskTitle,
			"미완료 사유를 입력해 주세요",
			"마감된 todo에 대한 미완료 사유가 필요합니다.",
			Map.of()
		));
	}

	@Override
	public void publishRetrospectiveReady(UUID groupId, UUID recipientUserId, UUID retrospectiveId, UUID weekId) {
		publishAfterCommit(NotificationCommandFactory.retrospectiveNotification(
			NotificationType.RETROSPECTIVE_READY,
			groupId,
			recipientUserId,
			retrospectiveId,
			weekId,
			"회고 피드백이 준비됐어요",
			"AI 팀장 피드백을 확인해 주세요."
		));
	}

	@Override
	public void publishNextWeekAdjusted(UUID groupId, UUID recipientUserId, UUID retrospectiveId, UUID weekId) {
		publishAfterCommit(NotificationCommandFactory.retrospectiveNotification(
			NotificationType.NEXT_WEEK_ADJUSTED,
			groupId,
			recipientUserId,
			retrospectiveId,
			weekId,
			"다음 주 조정안이 준비됐어요",
			"AI 팀장이 제안한 다음 주 학습 조정안을 확인해 주세요."
		));
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

	private void publishAfterCommit(CreateNotificationCommand command) {
		publishAfterCommit(() -> createNotificationSafely(command));
	}

	private void publishAfterCommit(Runnable task) {
		Objects.requireNonNull(task, "task must not be null");
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					runSafely(task);
				}
			});
			return;
		}
		runSafely(task);
	}

	private void createNotificationSafely(CreateNotificationCommand command) {
		try {
			createNotification(command);
		} catch (RuntimeException exception) {
			log.warn("in-app notification creation failed type={} idempotencyKey={}",
				command.notificationType(), command.idempotencyKey());
			log.debug("in-app notification creation failure detail", exception);
		}
	}

	private void runSafely(Runnable task) {
		try {
			task.run();
		} catch (RuntimeException exception) {
			log.warn("in-app notification event publishing failed");
			log.debug("in-app notification event publishing failure detail", exception);
		}
	}

	private static String redactError(String errorMessage) {
		String normalized = errorMessage == null ? "notification delivery failed." : errorMessage
			.replaceAll("(?i)sk-[a-z0-9_-]+", "[REDACTED]")
			.replaceAll("(?i)(token|secret|password|api[-_]?key)=\\S+", "$1=[REDACTED]")
			.replace('\n', ' ')
			.replace('\r', ' ')
			.strip();
		if (normalized.isBlank()) {
			return "notification delivery failed.";
		}
		if (normalized.length() <= MAX_ERROR_MESSAGE_LENGTH) {
			return normalized;
		}
		return normalized.substring(0, MAX_ERROR_MESSAGE_LENGTH);
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank.");
		}
		return value.strip();
	}
}
