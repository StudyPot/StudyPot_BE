package com.studypot.aistudyleader.identity.application;

public interface GoogleOAuthCodeExchangePort {

	GoogleOAuthProfile exchange(GoogleOAuthLoginCommand command);
}
