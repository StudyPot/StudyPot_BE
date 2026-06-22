package com.studypot.aistudyleader.review.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
class ReviewPersistenceConfiguration {

	@Bean
	ReviewRepository reviewRepository(JdbcTemplate jdbcTemplate) {
		return new JdbcReviewRepository(jdbcTemplate);
	}
}
