package com.studypot.aistudyleader.auth.infrastructure.security;

import com.studypot.aistudyleader.auth.service.AccessTokenIssuer;
import com.studypot.aistudyleader.auth.service.AuthSessionService;
import com.studypot.aistudyleader.auth.service.GoogleOAuthLoginService;
import com.studypot.aistudyleader.auth.repository.AuthAccountRepository;
import com.studypot.aistudyleader.auth.repository.RefreshTokenRepository;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthProperties.class)
class AuthSessionConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
	AuthSessionService authSessionService(
		GoogleOAuthLoginService googleOAuthLoginService,
		AuthAccountRepository authRepository,
		RefreshTokenRepository refreshTokenRepository,
		AccessTokenIssuer accessTokenIssuer,
		SecureRefreshTokenGenerator refreshTokenGenerator,
		AuthProperties properties,
		Clock authClock,
		@Qualifier("authUuidGenerator") Supplier<UUID> authUuidGenerator
	) {
		return new AuthSessionService(
			googleOAuthLoginService,
			authRepository,
			refreshTokenRepository,
			accessTokenIssuer,
			authClock,
			authUuidGenerator,
			refreshTokenGenerator,
			properties.refreshTokenTtl()
		);
	}
}
