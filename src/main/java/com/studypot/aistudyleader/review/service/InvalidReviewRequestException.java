package com.studypot.aistudyleader.review.service;

public class InvalidReviewRequestException extends RuntimeException {

	private final String field;

	public InvalidReviewRequestException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String field() {
		return field;
	}
}
