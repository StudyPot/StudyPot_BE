package com.studypot.aistudyleader.identity.adapter.out.persistence;

import com.studypot.aistudyleader.identity.application.IdentityAccountRepository;
import com.studypot.aistudyleader.identity.application.RefreshTokenRepository;
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
