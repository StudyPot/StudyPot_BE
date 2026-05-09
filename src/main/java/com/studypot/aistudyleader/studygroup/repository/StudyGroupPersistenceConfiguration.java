package com.studypot.aistudyleader.studygroup.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
class StudyGroupPersistenceConfiguration {

	@Bean
	StudyGroupRepository studyGroupRepository(JdbcTemplate jdbcTemplate, ObjectProvider<ObjectMapper> objectMapper) {
		return new JdbcStudyGroupRepository(jdbcTemplate, objectMapper.getIfAvailable(ObjectMapper::new));
	}
}
