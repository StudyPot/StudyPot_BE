package com.studypot.aistudyleader.identity.infrastructure.google;

import com.studypot.aistudyleader.identity.service.OAuthLoginRejectedException;

public class OAuthProviderResponseException extends OAuthLoginRejectedException {

	public OAuthProviderResponseException(String message) {
		super(message);
	}

	public OAuthProviderResponseException(String message, Throwable cause) {
		super(message, cause);
	}
}
