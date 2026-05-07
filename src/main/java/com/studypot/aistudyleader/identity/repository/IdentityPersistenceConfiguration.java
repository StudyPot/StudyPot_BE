package com.studypot.aistudyleader.identity.repository;

import com.studypot.aistudyleader.identity.repository.IdentityAccountRepository;
import com.studypot.aistudyleader.identity.repository.RefreshTokenRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
class IdentityPersistenceConfiguration {

	@Bean
	IdentityAccountRepository identityAccountRepository(JdbcTemplate jdbcTemplate) {
		return new JdbcIdentityAccountRepository(jdbcTemplate);
	}

	@Bean
	RefreshTokenRepository refreshTokenRepository(JdbcTemplate jdbcTemplate) {
		return new JdbcRefreshTokenRepository(jdbcTemplate);
	}
}
