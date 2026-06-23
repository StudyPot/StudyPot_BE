package com.studypot.aistudyleader.notification.service;

import java.time.Instant;
import java.util.UUID;

public interface NotificationEventPublisher {

	void publishOnboardingRequested(UUID groupId, UUID recipientUserId);

	void publishWeekStarted(UUID groupId, UUID weekId, int weekNumber, String weekTitle);

	void publishTaskDueReminder(UUID groupId, UUID recipientUserId, UUID weekId, UUID taskCompletionId, String taskTitle, Instant dueAt);

	void publishTaskOverdueCheck(UUID groupId, UUID recipientUserId, UUID taskCompletionId, String taskTitle);

	void publishIncompleteReasonRequested(UUID groupId, UUID recipientUserId, UUID taskCompletionId, String taskTitle);

	void publishRetrospectiveReady(UUID groupId, UUID recipientUserId, UUID retrospectiveId, UUID weekId);

	void publishNextWeekAdjusted(UUID groupId, UUID recipientUserId, UUID retrospectiveId, UUID weekId);

	// 아래 메서드들은 abstract 로 둔다. default(noop) 로 두면 in-process 발행자(NotificationService)가
	// 오버라이드를 누락해도 컴파일은 통과하면서 프로덕션에서 조용히 noop 이 되는 버그가 생긴다. (실제 발생함)
	void publishRetrospectiveReminder(UUID groupId, UUID recipientUserId, UUID weekId);

	void publishOnboardingCompleted(UUID groupId, UUID ownerUserId);

	void publishMemberJoined(UUID groupId, UUID ownerUserId, UUID joinedUserId);

	void publishOnboardingSubmitted(UUID groupId, UUID recipientUserId, UUID submitterMemberId);

	void publishGroupDeleted(UUID groupId, UUID recipientUserId, String groupName);

	static NotificationEventPublisher noop() {
		return NoOpNotificationEventPublisher.INSTANCE;
	}

	enum NoOpNotificationEventPublisher implements NotificationEventPublisher {
		INSTANCE;

		@Override
		public void publishOnboardingRequested(UUID groupId, UUID recipientUserId) {
		}

		@Override
		public void publishWeekStarted(UUID groupId, UUID weekId, int weekNumber, String weekTitle) {
		}

		@Override
		public void publishTaskDueReminder(UUID groupId, UUID recipientUserId, UUID weekId, UUID taskCompletionId, String taskTitle, Instant dueAt) {
		}

		@Override
		public void publishTaskOverdueCheck(UUID groupId, UUID recipientUserId, UUID taskCompletionId, String taskTitle) {
		}

		@Override
		public void publishIncompleteReasonRequested(UUID groupId, UUID recipientUserId, UUID taskCompletionId, String taskTitle) {
		}

		@Override
		public void publishRetrospectiveReady(UUID groupId, UUID recipientUserId, UUID retrospectiveId, UUID weekId) {
		}

		@Override
		public void publishNextWeekAdjusted(UUID groupId, UUID recipientUserId, UUID retrospectiveId, UUID weekId) {
		}

		@Override
		public void publishRetrospectiveReminder(UUID groupId, UUID recipientUserId, UUID weekId) {
		}

		@Override
		public void publishOnboardingCompleted(UUID groupId, UUID ownerUserId) {
		}

		@Override
		public void publishMemberJoined(UUID groupId, UUID ownerUserId, UUID joinedUserId) {
		}

		@Override
		public void publishOnboardingSubmitted(UUID groupId, UUID recipientUserId, UUID submitterMemberId) {
		}

		@Override
		public void publishGroupDeleted(UUID groupId, UUID recipientUserId, String groupName) {
		}
	}
}
