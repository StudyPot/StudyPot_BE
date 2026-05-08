package com.studypot.aistudyleader.auth.service;

public class AuthSessionRejectedException extends RuntimeException {

	public AuthSessionRejectedException(String message) {
		super(message);
	}
}
