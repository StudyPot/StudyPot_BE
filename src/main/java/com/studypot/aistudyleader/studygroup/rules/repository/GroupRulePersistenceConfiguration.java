package com.studypot.aistudyleader.studygroup.rules.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
class GroupRulePersistenceConfiguration {

	@Bean
	GroupRuleRepository groupRuleRepository(JdbcTemplate jdbcTemplate, ObjectProvider<ObjectMapper> objectMapper) {
		return new JdbcGroupRuleRepository(jdbcTemplate, objectMapper.getIfAvailable(ObjectMapper::new));
	}
}
