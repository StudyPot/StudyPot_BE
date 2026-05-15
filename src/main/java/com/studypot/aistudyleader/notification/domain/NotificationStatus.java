package com.studypot.aistudyleader.notification.domain;

public enum NotificationStatus {
	PENDING,
	DELIVERED,
	READ,
	FAILED,
	SKIPPED;

	public boolean canBeMarkedRead() {
		return this == DELIVERED || this == READ;
	}
}
