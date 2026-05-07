package com.studypot.aistudyleader.identity.adapter.out.security;

import com.studypot.aistudyleader.identity.application.AccessTokenIssuer;
import com.studypot.aistudyleader.identity.application.AuthSessionService;
import com.studypot.aistudyleader.identity.application.GoogleOAuthLoginService;
import com.studypot.aistudyleader.identity.application.IdentityAccountRepository;
import com.studypot.aistudyleader.identity.application.RefreshTokenRepository;
import com.studypot.aistudyleader.shared.domain.UuidV7;
import java.time.Clock;
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
		AuthProperties properties
	) {
		return new AuthSessionService(
			googleOAuthLoginService,
			identityRepository,
			refreshTokenRepository,
			accessTokenIssuer,
			Clock.systemUTC(),
			UuidV7::generate,
			refreshTokenGenerator,
			properties.refreshTokenTtl()
		);
	}
}
