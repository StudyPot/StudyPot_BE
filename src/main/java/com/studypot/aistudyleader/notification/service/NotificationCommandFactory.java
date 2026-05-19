package com.studypot.aistudyleader.notification.service;

import com.studypot.aistudyleader.notification.domain.NotificationRelatedResources;
import com.studypot.aistudyleader.notification.domain.NotificationType;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class NotificationCommandFactory {

	private NotificationCommandFactory() {
	}

	static CreateNotificationCommand onboardingRequested(UUID groupId, UUID recipientUserId) {
		return new CreateNotificationCommand(
			groupId,
			recipientUserId,
			null,
			NotificationType.ONBOARDING_REQUESTED,
			"idempotency:notification:onboarding-requested:group:%s:recipient:%s".formatted(groupId, recipientUserId),
			"온보딩을 작성해 주세요",
			"스터디 시작 전 온보딩을 완료해 주세요.",
			payload("/groups/%s/onboarding".formatted(groupId), Map.of("groupId", groupId.toString())),
			null
		);
	}

	static CreateNotificationCommand weekStarted(
		UUID groupId,
		UUID recipientUserId,
		UUID weekId,
		int weekNumber,
		String weekTitle
	) {
		if (weekNumber <= 0) {
			throw new IllegalArgumentException("weekNumber must be positive.");
		}
		return new CreateNotificationCommand(
			groupId,
			recipientUserId,
			new NotificationRelatedResources(null, weekId, null, null),
			NotificationType.WEEK_STARTED,
			"idempotency:notification:week-started:week:%s:recipient:%s".formatted(weekId, recipientUserId),
			"%d주차 학습이 시작됐어요".formatted(weekNumber),
			weekTitle + " todo를 확인해 주세요.",
			payload("/weeks/%s/tasks".formatted(weekId), Map.of(
				"groupId", groupId.toString(),
				"weekId", weekId.toString(),
				"weekNumber", weekNumber,
				"weekTitle", weekTitle
			)),
			null
		);
	}

	static CreateNotificationCommand taskNotification(
		NotificationType type,
		UUID groupId,
		UUID recipientUserId,
		UUID weekId,
		UUID taskCompletionId,
		String taskTitle,
		String title,
		String body,
		Map<String, Object> extraPayload
	) {
		Objects.requireNonNull(taskCompletionId, "taskCompletionId must not be null");
		String safeTaskTitle = requireText(taskTitle, "taskTitle");
		Map<String, Object> values = new LinkedHashMap<>(extraPayload);
		values.put("groupId", groupId.toString());
		if (weekId != null) {
			values.put("weekId", weekId.toString());
		}
		values.put("taskCompletionId", taskCompletionId.toString());
		values.put("taskTitle", safeTaskTitle);
		return new CreateNotificationCommand(
			groupId,
			recipientUserId,
			new NotificationRelatedResources(null, weekId, taskCompletionId, null),
			type,
			"idempotency:notification:%s:task-completion:%s:recipient:%s".formatted(
				type.name().toLowerCase(Locale.ROOT),
				taskCompletionId,
				recipientUserId
			),
			title,
			body,
			payload("/task-completions/%s".formatted(taskCompletionId), values),
			null
		);
	}

	static CreateNotificationCommand retrospectiveNotification(
		NotificationType type,
		UUID groupId,
		UUID recipientUserId,
		UUID retrospectiveId,
		UUID weekId,
		String title,
		String body
	) {
		Objects.requireNonNull(retrospectiveId, "retrospectiveId must not be null");
		Objects.requireNonNull(weekId, "weekId must not be null");
		return new CreateNotificationCommand(
			groupId,
			recipientUserId,
			new NotificationRelatedResources(null, weekId, null, retrospectiveId),
			type,
			"idempotency:notification:%s:retrospective:%s:recipient:%s".formatted(
				type.name().toLowerCase(Locale.ROOT),
				retrospectiveId,
				recipientUserId
			),
			title,
			body,
			payload("/weeks/%s/retrospectives/me".formatted(weekId), Map.of(
				"groupId", groupId.toString(),
				"weekId", weekId.toString(),
				"retrospectiveId", retrospectiveId.toString()
			)),
			null
		);
	}

	private static Map<String, Object> payload(String deepLink, Map<String, Object> values) {
		Map<String, Object> payload = new LinkedHashMap<>(values);
		payload.put("deepLink", deepLink);
		return Map.copyOf(payload);
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank.");
		}
		return value.strip();
	}
}
