package com.studypot.aistudyleader.notification.service;

import com.studypot.aistudyleader.notification.domain.Notification;

@FunctionalInterface
public interface NotificationStreamPublisher {

	void publishNotificationCreated(Notification notification);

	static NotificationStreamPublisher noop() {
		return notification -> {
		};
	}
}
