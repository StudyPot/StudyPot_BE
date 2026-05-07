package com.studypot.aistudyleader.identity.service;

public class AuthSessionRejectedException extends RuntimeException {

	public AuthSessionRejectedException(String message) {
		super(message);
	}
}
