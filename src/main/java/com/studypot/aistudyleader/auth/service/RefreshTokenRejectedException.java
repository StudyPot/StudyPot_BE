package com.studypot.aistudyleader.auth.service;

public class RefreshTokenRejectedException extends AuthSessionRejectedException {

	private static final String DEFAULT_CODE = "REFRESH_TOKEN_INVALID";

	private final String code;

	public RefreshTokenRejectedException(String message) {
		this(message, DEFAULT_CODE);
	}

	private RefreshTokenRejectedException(String message, String code) {
		super(message);
		this.code = code;
	}

	public static RefreshTokenRejectedException required() {
		return new RefreshTokenRejectedException("refresh token is required.", "REFRESH_TOKEN_REQUIRED");
	}

	public static RefreshTokenRejectedException invalid() {
		return new RefreshTokenRejectedException("refresh token is invalid.", "REFRESH_TOKEN_INVALID");
	}

	public static RefreshTokenRejectedException expired() {
		return new RefreshTokenRejectedException("refresh token is expired.", "REFRESH_TOKEN_EXPIRED");
	}

	public static RefreshTokenRejectedException invalidOrRevoked() {
		return new RefreshTokenRejectedException("refresh token is invalid or revoked.", "REFRESH_TOKEN_INVALID");
	}

	public String code() {
		return code;
	}
}
