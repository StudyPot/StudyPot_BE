package com.studypot.aistudyleader.review.repository;

public class ReviewPersistenceException extends RuntimeException {

	public ReviewPersistenceException(String message) {
		super(message);
	}

	public ReviewPersistenceException(String message, Throwable cause) {
		super(message, cause);
	}
}
