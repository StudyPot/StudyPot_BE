package com.studypot.aistudyleader.identity.service;

import com.studypot.aistudyleader.global.domain.UuidV7;
import com.studypot.aistudyleader.identity.repository.IdentityAccountRepository;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class IdentityApplicationConfiguration {

	@Bean
	@ConditionalOnBean({GoogleOAuthCodeExchangePort.class, IdentityAccountRepository.class})
	GoogleOAuthLoginService googleOAuthLoginService(
		GoogleOAuthCodeExchangePort googleOAuth,
		IdentityAccountRepository repository
	) {
		return new GoogleOAuthLoginService(googleOAuth, repository, Clock.systemUTC(), UuidV7::generate);
	}
}
