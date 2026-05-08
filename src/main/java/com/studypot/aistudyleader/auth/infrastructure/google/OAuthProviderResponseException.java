package com.studypot.aistudyleader.auth.infrastructure.google;

import com.studypot.aistudyleader.auth.service.OAuthLoginRejectedException;

public class OAuthProviderResponseException extends OAuthLoginRejectedException {

	public OAuthProviderResponseException(String message) {
		super(message);
	}

	public OAuthProviderResponseException(String message, Throwable cause) {
		super(message, cause);
	}
}
