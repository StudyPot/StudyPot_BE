package com.studypot.aistudyleader.notification.service;

import com.studypot.aistudyleader.notification.domain.Notification;
import com.studypot.aistudyleader.notification.domain.NotificationChannel;
import com.studypot.aistudyleader.notification.domain.NotificationRelatedResources;
import com.studypot.aistudyleader.notification.domain.NotificationStatus;
import com.studypot.aistudyleader.notification.domain.NotificationType;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record CreateNotificationCommand(
	UUID groupId,
	UUID recipientUserId,
	NotificationRelatedResources relatedResources,
	NotificationType notificationType,
	String idempotencyKey,
	String title,
	String body,
	Map<String, Object> payload,
	Instant scheduledAt
) {

	public CreateNotificationCommand {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
		relatedResources = relatedResources == null ? new NotificationRelatedResources(null, null, null, null) : relatedResources;
		Objects.requireNonNull(notificationType, "notificationType must not be null");
		idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
		title = requireText(title, "title");
		body = normalizeOptionalText(body);
		payload = copyPayload(payload);
	}

	Notification toDeliveredNotification(UUID id, Instant deliveredAt) {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(deliveredAt, "deliveredAt must not be null");
		return new Notification(
			id,
			groupId,
			recipientUserId,
			relatedResources,
			notificationType,
			NotificationChannel.IN_APP,
			idempotencyKey,
			title,
			body,
			payload,
			NotificationStatus.DELIVERED,
			deliveredAt,
			null,
			null,
			0,
			scheduledAt,
			deliveredAt
		);
	}

	Notification toFailedNotification(UUID id, String errorMessage, Instant failedAt) {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(failedAt, "failedAt must not be null");
		return new Notification(
			id,
			groupId,
			recipientUserId,
			relatedResources,
			notificationType,
			NotificationChannel.IN_APP,
			idempotencyKey,
			title,
			body,
			payload,
			NotificationStatus.FAILED,
			null,
			null,
			errorMessage,
			1,
			scheduledAt,
			failedAt
		);
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank.");
		}
		return value.strip();
	}

	private static String normalizeOptionalText(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

	private static Map<String, Object> copyPayload(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return Map.of();
		}
		for (Map.Entry<String, Object> entry : payload.entrySet()) {
			if (entry.getKey() == null || entry.getValue() == null) {
				throw new IllegalArgumentException("payload must not contain null keys or values.");
			}
		}
		return Map.copyOf(new LinkedHashMap<>(payload));
	}
}
