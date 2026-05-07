package com.studypot.aistudyleader.identity.service;

public interface GoogleOAuthCodeExchangePort {

	GoogleOAuthProfile exchange(GoogleOAuthLoginCommand command);
}
