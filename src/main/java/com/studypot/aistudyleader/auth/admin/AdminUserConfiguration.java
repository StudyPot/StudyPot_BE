package com.studypot.aistudyleader.auth.admin;

import com.studypot.aistudyleader.auth.repository.AuthAccountRepository;
import com.studypot.aistudyleader.llm.admin.AdminProperties;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 운영자 사용자 요금제 관리 서비스 구성입니다.
 * 영속성(AuthPersistenceConfiguration)과 동일한 프로퍼티 게이트로 활성화되어,
 * 데이터소스가 구성된 환경에서만 빈을 노출합니다.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdminProperties.class)
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
class AdminUserConfiguration {

	@Bean
	AdminUserService adminUserService(AuthAccountRepository authRepository, AdminProperties adminProperties) {
		return new AdminUserService(authRepository, adminProperties, Clock.systemUTC());
	}
}
