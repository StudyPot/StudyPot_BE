package com.studypot.aistudyleader.ai.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
class AiConversationPersistenceConfiguration {

	@Bean
	AiConversationRepository aiConversationRepository(JdbcTemplate jdbcTemplate) {
		return new JdbcAiConversationRepository(jdbcTemplate);
	}
}
