package com.studypot.aistudyleader.identity.service;

public class IdentityUniquenessConflictException extends RuntimeException {

	public IdentityUniquenessConflictException(String message, Throwable cause) {
		super(message, cause);
	}

	public IdentityUniquenessConflictException(String message) {
		super(message);
	}
}
