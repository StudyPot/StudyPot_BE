package com.studypot.aistudyleader.studygroup.catalog;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
class StudyGroupCatalogPersistenceConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
	StudyGroupCatalogMapper jdbcStudyGroupCatalogMapper(JdbcTemplate jdbcTemplate) {
		return new JdbcStudyGroupCatalogMapper(jdbcTemplate);
	}

	@Bean
	@ConditionalOnMissingBean(StudyGroupCatalogMapper.class)
	StudyGroupCatalogMapper inMemoryStudyGroupCatalogMapper() {
		return new InMemoryStudyGroupCatalogMapper();
	}
}
