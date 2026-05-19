package com.studypot.aistudyleader.notification.service;

public interface NotificationJobPublisher {

	void publish(CreateNotificationCommand command);
}
