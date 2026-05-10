package com.studypot.aistudyleader.auth.repository;

public class AuthUniquenessConflictException extends RuntimeException {

	public AuthUniquenessConflictException(String message, Throwable cause) {
		super(message, cause);
	}

	public AuthUniquenessConflictException(String message) {
		super(message);
	}
}
