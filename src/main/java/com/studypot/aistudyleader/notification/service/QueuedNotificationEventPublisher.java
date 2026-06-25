package com.studypot.aistudyleader.notification.service;

import com.studypot.aistudyleader.notification.domain.NotificationType;
import com.studypot.aistudyleader.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class QueuedNotificationEventPublisher implements NotificationEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(QueuedNotificationEventPublisher.class);

	private final NotificationRepository repository;
	private final NotificationJobPublisher jobs;

	public QueuedNotificationEventPublisher(NotificationRepository repository, NotificationJobPublisher jobs) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.jobs = Objects.requireNonNull(jobs, "jobs must not be null");
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
				publishSafely(NotificationCommandFactory.weekStarted(groupId, recipientUserId, weekId, weekNumber, safeWeekTitle));
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
				publishSafely(NotificationCommandFactory.noticePosted(groupId, recipientUserId, postId, safeTitle));
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
				publishSafely(NotificationCommandFactory.leaderReportPosted(groupId, recipientUserId, postId, safeTitle));
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
				publishSafely(NotificationCommandFactory.studyCompleted(groupId, recipientUserId, safeName));
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

	private void publishAfterCommit(CreateNotificationCommand command) {
		publishAfterCommit(() -> publishSafely(command));
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

	private void publishSafely(CreateNotificationCommand command) {
		try {
			jobs.publish(command);
		} catch (RuntimeException exception) {
			log.warn("in-app notification job enqueue failed type={} idempotencyKey={}",
				command.notificationType(), command.idempotencyKey());
			log.debug("in-app notification job enqueue failure detail", exception);
		}
	}

	private void runSafely(Runnable task) {
		try {
			task.run();
		} catch (RuntimeException exception) {
			log.warn("in-app notification event enqueue failed");
			log.debug("in-app notification event enqueue failure detail", exception);
		}
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank.");
		}
		return value.strip();
	}
}
