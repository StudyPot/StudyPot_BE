package com.studypot.aistudyleader.identity.service;

public class RefreshTokenRejectedException extends AuthSessionRejectedException {

	public RefreshTokenRejectedException(String message) {
		super(message);
	}
}
