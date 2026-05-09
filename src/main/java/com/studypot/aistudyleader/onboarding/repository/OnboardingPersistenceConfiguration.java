package com.studypot.aistudyleader.onboarding.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
class OnboardingPersistenceConfiguration {

	@Bean
	@ConditionalOnBean(JdbcTemplate.class)
	OnboardingRepository onboardingRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		return new JdbcOnboardingRepository(jdbcTemplate, objectMapper);
	}
}
