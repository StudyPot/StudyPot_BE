package com.studypot.aistudyleader.identity.service;

import com.studypot.aistudyleader.global.domain.UuidV7;
import com.studypot.aistudyleader.identity.repository.IdentityAccountRepository;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class IdentityApplicationConfiguration {

	@Bean
	@ConditionalOnBean({GoogleOAuthCodeExchangePort.class, IdentityAccountRepository.class})
	GoogleOAuthLoginService googleOAuthLoginService(
		GoogleOAuthCodeExchangePort googleOAuth,
		IdentityAccountRepository repository,
		Clock identityClock,
		@Qualifier("identityUuidGenerator") Supplier<UUID> identityUuidGenerator
	) {
		return new GoogleOAuthLoginService(googleOAuth, repository, identityClock, identityUuidGenerator);
	}

	@Bean
	@ConditionalOnMissingBean(Clock.class)
	Clock identityClock() {
		return Clock.systemUTC();
	}

	@Bean
	@ConditionalOnMissingBean(name = "identityUuidGenerator")
	Supplier<UUID> identityUuidGenerator() {
		return UuidV7::generate;
	}
}
