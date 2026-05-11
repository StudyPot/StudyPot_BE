package com.studypot.aistudyleader.curriculum.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
class CurriculumPersistenceConfiguration {

	@Bean
	@ConditionalOnBean(JdbcTemplate.class)
	CurriculumRepository curriculumRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		return new JdbcCurriculumRepository(jdbcTemplate, objectMapper);
	}
}
