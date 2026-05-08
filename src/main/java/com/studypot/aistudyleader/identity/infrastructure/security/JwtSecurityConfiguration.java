package com.studypot.aistudyleader.identity.infrastructure.security;

import com.studypot.aistudyleader.identity.service.AccessTokenIssuer;
import com.studypot.aistudyleader.identity.service.AuthTokenCookiePort;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthProperties.class)
class JwtSecurityConfiguration {

	@Bean
	SecretKey studypotJwtSecretKey(AuthProperties properties) {
		String secret = properties.jwt().secret();
		if (secret == null || secret.isBlank() || secret.length() < 32) {
			throw new IllegalStateException("studypot.auth.jwt.secret must be at least 32 characters.");
		}
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey studypotJwtSecretKey) {
		return NimbusJwtEncoder.withSecretKey(studypotJwtSecretKey)
			.build();
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey studypotJwtSecretKey) {
		return NimbusJwtDecoder.withSecretKey(studypotJwtSecretKey)
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
	}

	@Bean
	AccessTokenIssuer accessTokenIssuer(JwtEncoder jwtEncoder, AuthProperties properties) {
		return new JwtAccessTokenIssuer(jwtEncoder, properties);
	}

	@Bean
	AuthTokenCookiePort authTokenCookiePort(AuthProperties properties) {
		return new AuthTokenCookieIssuer(properties);
	}

	@Bean
	BearerTokenResolver bearerTokenResolver(AuthTokenCookiePort authTokenCookiePort) {
		return new CookieBearerTokenResolver(authTokenCookiePort);
	}

	@Bean
	SecureRefreshTokenGenerator secureRefreshTokenGenerator() {
		return new SecureRefreshTokenGenerator();
	}
}
