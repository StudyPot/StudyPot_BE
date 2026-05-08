package com.studypot.aistudyleader.auth.service;

public interface GoogleOAuthCodeExchangePort {

	GoogleOAuthProfile exchange(GoogleOAuthLoginCommand command);
}
