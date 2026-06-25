package com.studypot.aistudyleader.bookmark.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
class BookmarkPersistenceConfiguration {

	@Bean
	BookmarkRepository bookmarkRepository(JdbcTemplate jdbcTemplate, ObjectProvider<ObjectMapper> objectMapper) {
		return new JdbcBookmarkRepository(jdbcTemplate, objectMapper.getIfAvailable(() -> JsonMapper.builder().findAndAddModules().build()));
	}
}
