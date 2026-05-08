package com.studypot.aistudyleader.identity.infrastructure.security;

import com.studypot.aistudyleader.identity.service.AuthSessionService;
import com.studypot.aistudyleader.identity.service.AuthTokenCookiePort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class GoogleOAuth2LoginHandlerConfiguration {

	@Bean
	GoogleOAuth2LoginSuccessHandler googleOAuth2LoginSuccessHandler(
		ObjectProvider<AuthSessionService> authSessionService,
		AuthTokenCookiePort tokenCookiePort,
		AuthProperties properties
	) {
		return new GoogleOAuth2LoginSuccessHandler(authSessionService, tokenCookiePort, properties);
	}

	@Bean
	GoogleOAuth2LoginFailureHandler googleOAuth2LoginFailureHandler(AuthProperties properties) {
		return new GoogleOAuth2LoginFailureHandler(properties);
	}
}
