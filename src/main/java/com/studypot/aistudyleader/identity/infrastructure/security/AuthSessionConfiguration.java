package com.studypot.aistudyleader.identity.infrastructure.security;

import com.studypot.aistudyleader.identity.service.AccessTokenIssuer;
import com.studypot.aistudyleader.identity.service.AuthSessionService;
import com.studypot.aistudyleader.identity.service.GoogleOAuthLoginService;
import com.studypot.aistudyleader.identity.repository.IdentityAccountRepository;
import com.studypot.aistudyleader.identity.repository.RefreshTokenRepository;
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
		IdentityAccountRepository identityRepository,
		RefreshTokenRepository refreshTokenRepository,
		AccessTokenIssuer accessTokenIssuer,
		SecureRefreshTokenGenerator refreshTokenGenerator,
		AuthProperties properties,
		Clock identityClock,
		@Qualifier("identityUuidGenerator") Supplier<UUID> identityUuidGenerator
	) {
		return new AuthSessionService(
			googleOAuthLoginService,
			identityRepository,
			refreshTokenRepository,
			accessTokenIssuer,
			identityClock,
			identityUuidGenerator,
			refreshTokenGenerator,
			properties.refreshTokenTtl()
		);
	}
}
