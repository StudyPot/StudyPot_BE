package com.studypot.aistudyleader.studygroup.rules.service;

public class InvalidGroupRuleRequestException extends RuntimeException {

	private final String field;

	public InvalidGroupRuleRequestException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String field() {
		return field;
	}
}
