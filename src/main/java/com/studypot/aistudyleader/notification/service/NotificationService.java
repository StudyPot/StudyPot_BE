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
	private final NotificationStreamPublisher streamPublisher;

	public NotificationService(NotificationRepository repository, Clock clock) {
		this(repository, clock, UuidV7::generate);
	}

	public NotificationService(NotificationRepository repository, Clock clock, Supplier<UUID> idGenerator) {
		this(repository, clock, idGenerator, NotificationStreamPublisher.noop());
	}

	public NotificationService(
		NotificationRepository repository,
		Clock clock,
		Supplier<UUID> idGenerator,
		NotificationStreamPublisher streamPublisher
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
		this.streamPublisher = Objects.requireNonNull(streamPublisher, "streamPublisher must not be null");
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
		PersistedNotification persisted = persistNotification(command);
		if (persisted.newlyCreated()) {
			// 워커(RabbitMQ) 경로: 이 메서드는 프록시를 통해 자체 트랜잭션에서 실행되므로
			// 트랜잭션 커밋 후 SSE 를 푸시하도록 afterCommit 으로 등록한다.
			publishAfterCommit(() -> streamPublisher.publishNotificationCreated(persisted.notification()));
		}
		return persisted.notification();
	}

	private PersistedNotification persistNotification(CreateNotificationCommand command) {
		Notification candidate = command.toDeliveredNotification(idGenerator.get(), clock.instant());
		Notification savedNotification = repository.saveNotification(candidate);
		return new PersistedNotification(savedNotification, savedNotification.id().equals(candidate.id()));
	}

	private record PersistedNotification(Notification notification, boolean newlyCreated) {
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
	public void publishNoticePosted(UUID groupId, UUID actorUserId, UUID postId, String title) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(actorUserId, "actorUserId must not be null");
		Objects.requireNonNull(postId, "postId must not be null");
		String safeTitle = requireText(title, "title");
		publishAfterCommit(() -> {
			List<UUID> recipients = repository.findActiveGroupRecipientUserIds(groupId);
			for (UUID recipientUserId : recipients) {
				if (recipientUserId.equals(actorUserId)) {
					continue;
				}
				createNotificationSafely(NotificationCommandFactory.noticePosted(groupId, recipientUserId, postId, safeTitle));
			}
		});
	}

	@Override
	public void publishLeaderReportPosted(UUID groupId, UUID postId, String title) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(postId, "postId must not be null");
		String safeTitle = requireText(title, "title");
		publishAfterCommit(() -> {
			List<UUID> recipients = repository.findActiveGroupRecipientUserIds(groupId);
			for (UUID recipientUserId : recipients) {
				createNotificationSafely(NotificationCommandFactory.leaderReportPosted(groupId, recipientUserId, postId, safeTitle));
			}
		});
	}

	@Override
	public void publishStudyCompleted(UUID groupId, String groupName) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		String safeName = requireText(groupName, "groupName");
		publishAfterCommit(() -> {
			List<UUID> recipients = repository.findActiveGroupRecipientUserIds(groupId);
			for (UUID recipientUserId : recipients) {
				createNotificationSafely(NotificationCommandFactory.studyCompleted(groupId, recipientUserId, safeName));
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

	@Override
	public void publishOnboardingCompleted(UUID groupId, UUID ownerUserId) {
		publishAfterCommit(NotificationCommandFactory.onboardingCompleted(groupId, ownerUserId));
	}

	@Override
	public void publishMemberJoined(UUID groupId, UUID ownerUserId, UUID joinedUserId) {
		publishAfterCommit(NotificationCommandFactory.memberJoined(groupId, ownerUserId, joinedUserId));
	}

	@Override
	public void publishOnboardingSubmitted(UUID groupId, UUID recipientUserId, UUID submitterMemberId) {
		publishAfterCommit(NotificationCommandFactory.onboardingSubmitted(groupId, recipientUserId, submitterMemberId));
	}

	@Override
	public void publishGroupDeleted(UUID groupId, UUID recipientUserId, String groupName) {
		publishAfterCommit(NotificationCommandFactory.groupDeleted(groupId, recipientUserId, groupName));
	}

	@Override
	public void publishRetrospectiveReminder(UUID groupId, UUID recipientUserId, UUID weekId) {
		publishAfterCommit(NotificationCommandFactory.retrospectiveReminder(
			groupId,
			recipientUserId,
			weekId,
			"이번 주 회고를 작성해 주세요",
			"이번 주차 마감이 한 시간 남았어요. AI 팀장과 함께 회고를 시작해 보세요."
		));
	}

	@Override
	public void publishRetrospectiveFinalReminder(UUID groupId, UUID recipientUserId, UUID weekId) {
		publishAfterCommit(NotificationCommandFactory.retrospectiveFinalReminder(
			groupId,
			recipientUserId,
			weekId,
			"회고 마감이 곧 끝나요",
			"잠시 후 이번 주차 리포트가 만들어져요. 그 전에 아직 작성하지 않은 회고를 마무리해 주세요."
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
			PersistedNotification persisted = persistNotification(command);
			if (persisted.newlyCreated()) {
				// in-process 발행 경로: 이 메서드는 업무 트랜잭션의 afterCommit 단계에서 실행된다.
				// 여기서 SSE 푸시를 다시 afterCommit 으로 등록하면, 이미 진행 중인 afterCommit 콜백
				// 순회에 잡히지 않아 푸시가 유실된다(=실시간 알림이 안 가고 30초 폴링으로만 옴).
				// 이미 커밋이 끝난 시점이므로 저장 직후 즉시 푸시한다.
				streamPublisher.publishNotificationCreated(persisted.notification());
			}
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
