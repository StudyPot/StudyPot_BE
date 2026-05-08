package com.studypot.aistudyleader.identity.service;

public class OAuthLoginRejectedException extends RuntimeException {

	public OAuthLoginRejectedException(String message) {
		super(message);
	}

	public OAuthLoginRejectedException(String message, Throwable cause) {
		super(message, cause);
	}
}
