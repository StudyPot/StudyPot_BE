package com.studypot.aistudyleader.studygroup.board.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
class GroupBoardPersistenceConfiguration {

	@Bean
	GroupBoardRepository groupBoardRepository(JdbcTemplate jdbcTemplate, ObjectProvider<ObjectMapper> objectMapper) {
		return new JdbcGroupBoardRepository(
			jdbcTemplate,
			objectMapper.getIfAvailable(() -> JsonMapper.builder().findAndAddModules().build())
		);
	}
}
