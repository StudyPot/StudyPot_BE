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

	default void publishRetrospectiveReminder(UUID groupId, UUID recipientUserId, UUID weekId) {
	}

	default void publishOnboardingCompleted(UUID groupId, UUID ownerUserId) {
	}

	default void publishMemberJoined(UUID groupId, UUID ownerUserId, UUID joinedUserId) {
	}

	default void publishOnboardingSubmitted(UUID groupId, UUID recipientUserId, UUID submitterMemberId) {
	}

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
	}
}
