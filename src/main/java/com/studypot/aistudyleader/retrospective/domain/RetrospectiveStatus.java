package com.studypot.aistudyleader.retrospective.domain;

public enum RetrospectiveStatus {
	PENDING,
	PROCESSING,
	COMPLETED,
	FAILED;

	public boolean isProcessing() {
		return this == PROCESSING;
	}

	public boolean isCompleted() {
		return this == COMPLETED;
	}
}
