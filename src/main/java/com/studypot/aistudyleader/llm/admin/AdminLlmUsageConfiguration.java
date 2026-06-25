package com.studypot.aistudyleader.llm.admin;

import com.studypot.aistudyleader.llm.repository.LlmUsageRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdminProperties.class)
class AdminLlmUsageConfiguration {

	@Bean
	@ConditionalOnBean(LlmUsageRepository.class)
	AdminLlmUsageService adminLlmUsageService(LlmUsageRepository repository, AdminProperties adminProperties) {
		return new AdminLlmUsageService(repository, adminProperties);
	}
}
