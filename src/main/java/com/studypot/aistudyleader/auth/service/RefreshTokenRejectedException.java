package com.studypot.aistudyleader.auth.service;

public class RefreshTokenRejectedException extends AuthSessionRejectedException {

	public RefreshTokenRejectedException(String message) {
		super(message);
	}
}
