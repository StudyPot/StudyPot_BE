package com.studypot.aistudyleader.auth.service;

import com.studypot.aistudyleader.global.domain.UuidV7;
import com.studypot.aistudyleader.auth.repository.AuthAccountRepository;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
class AuthApplicationConfiguration {

	@Bean
	@ConditionalOnBean({GoogleOAuthCodeExchangePort.class, AuthAccountRepository.class})
	GoogleOAuthLoginService googleOAuthLoginService(
		GoogleOAuthCodeExchangePort googleOAuth,
		AuthAccountRepository repository,
		Clock authClock,
		@Qualifier("authUuidGenerator") Supplier<UUID> authUuidGenerator
	) {
		return new GoogleOAuthLoginService(googleOAuth, repository, authClock, authUuidGenerator);
	}

	@Bean
	@ConditionalOnMissingBean(Clock.class)
	Clock authClock() {
		return Clock.systemUTC();
	}

	@Bean
	@ConditionalOnMissingBean(name = "authUuidGenerator")
	Supplier<UUID> authUuidGenerator() {
		return UuidV7::generate;
	}

	@Bean
	@ConditionalOnMissingBean(PasswordEncoder.class)
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	@ConditionalOnBean(AuthAccountRepository.class)
	SignupService signupService(
		AuthAccountRepository repository,
		Clock authClock,
		@Qualifier("authUuidGenerator") Supplier<UUID> authUuidGenerator,
		PasswordEncoder passwordEncoder
	) {
		return new SignupService(repository, authClock, authUuidGenerator, passwordEncoder);
	}
}
