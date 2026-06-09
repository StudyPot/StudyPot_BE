package com.studypot.aistudyleader.auth.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.auth.repository.AuthAccountRepository;
import com.studypot.aistudyleader.auth.repository.RefreshTokenRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
class AuthPersistenceConfiguration {

	@Bean
	AuthAccountRepository authAccountRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		return new JdbcAuthAccountRepository(jdbcTemplate, objectMapper);
	}

	@Bean
	RefreshTokenRepository refreshTokenRepository(JdbcTemplate jdbcTemplate) {
		return new JdbcRefreshTokenRepository(jdbcTemplate);
	}
}
