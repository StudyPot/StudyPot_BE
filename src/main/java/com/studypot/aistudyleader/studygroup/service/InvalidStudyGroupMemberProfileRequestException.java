package com.studypot.aistudyleader.studygroup.service;

public class InvalidStudyGroupMemberProfileRequestException extends RuntimeException {

	private final String field;

	public InvalidStudyGroupMemberProfileRequestException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String field() {
		return field;
	}
}
