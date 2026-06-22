package com.studypot.aistudyleader.review;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
class ReviewPersistenceConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
	ReviewRepository jdbcReviewRepository(JdbcTemplate jdbcTemplate) {
		return new JdbcReviewRepository(jdbcTemplate);
	}

	@Bean
	@ConditionalOnMissingBean(ReviewRepository.class)
	ReviewRepository inMemoryReviewRepository() {
		return new InMemoryReviewRepository();
	}
}
