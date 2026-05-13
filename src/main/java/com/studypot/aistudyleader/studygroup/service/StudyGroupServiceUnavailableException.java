package com.studypot.aistudyleader.studygroup.service;

public class StudyGroupServiceUnavailableException extends RuntimeException {

	public StudyGroupServiceUnavailableException(String message) {
		super(message);
	}

	public StudyGroupServiceUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
