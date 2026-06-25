package com.studypot.aistudyleader.llm.admin;

import com.studypot.aistudyleader.llm.repository.LlmUsageRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdminProperties.class)
// 리포지토리(LlmUsagePersistenceConfiguration)와 동일한 프로퍼티 게이트를 쓴다.
// @ConditionalOnBean(LlmUsageRepository) 는 설정 클래스 스캔 순서에 의존해
// llm.admin 이 llm.repository 보다 먼저 처리되면 빈이 누락(→ 운영 API 503)되므로,
// 순서와 무관한 @ConditionalOnProperty 로 리포지토리와 항상 함께 활성화되도록 한다.
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
class AdminLlmUsageConfiguration {

	@Bean
	AdminLlmUsageService adminLlmUsageService(LlmUsageRepository repository, AdminProperties adminProperties) {
		return new AdminLlmUsageService(repository, adminProperties);
	}
}
