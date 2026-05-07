package com.studypot.aistudyleader.identity.application;

public class RefreshTokenRejectedException extends AuthSessionRejectedException {

	public RefreshTokenRejectedException(String message) {
		super(message);
	}
}
