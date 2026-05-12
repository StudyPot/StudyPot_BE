package com.studypot.aistudyleader.studygroup.rules.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
class GroupRulePersistenceConfiguration {

	@Bean
	@ConditionalOnBean(JdbcTemplate.class)
	GroupRuleRepository groupRuleRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		return new JdbcGroupRuleRepository(jdbcTemplate, objectMapper);
	}
}
