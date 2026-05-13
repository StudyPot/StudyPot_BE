package com.studypot.aistudyleader.notification.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record Notification(
	UUID id,
	UUID groupId,
	UUID recipientUserId,
	NotificationRelatedResources relatedResources,
	NotificationType notificationType,
	NotificationChannel channel,
	String idempotencyKey,
	String title,
	String body,
	Map<String, Object> payload,
	NotificationStatus status,
	Instant deliveredAt,
	Instant readAt,
	String errorMessage,
	int retryCount,
	Instant scheduledAt,
	Instant createdAt
) {

	public Notification {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
		relatedResources = relatedResources == null ? new NotificationRelatedResources(null, null, null, null) : relatedResources;
		Objects.requireNonNull(notificationType, "notificationType must not be null");
		Objects.requireNonNull(channel, "channel must not be null");
		idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
		title = requireText(title, "title");
		body = normalizeOptionalText(body);
		payload = copyPayload(payload);
		Objects.requireNonNull(status, "status must not be null");
		if (status == NotificationStatus.READ && deliveredAt == null) {
			throw new IllegalArgumentException("deliveredAt is required when notification status is READ.");
		}
		if (status == NotificationStatus.READ && readAt == null) {
			throw new IllegalArgumentException("readAt is required when notification status is READ.");
		}
		errorMessage = normalizeOptionalText(errorMessage);
		if (retryCount < 0) {
			throw new IllegalArgumentException("retryCount must not be negative.");
		}
		Objects.requireNonNull(createdAt, "createdAt must not be null");
	}

	public boolean belongsToRecipient(UUID userId) {
		return recipientUserId.equals(userId);
	}

	public Notification markRead(Instant now) {
		Objects.requireNonNull(now, "now must not be null");
		if (status == NotificationStatus.READ) {
			return this;
		}
		if (!status.canBeMarkedRead()) {
			throw new IllegalStateException("only delivered notifications can be marked read.");
		}
		return new Notification(
			id,
			groupId,
			recipientUserId,
			relatedResources,
			notificationType,
			channel,
			idempotencyKey,
			title,
			body,
			payload,
			NotificationStatus.READ,
			deliveredAt,
			now,
			errorMessage,
			retryCount,
			scheduledAt,
			createdAt
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
