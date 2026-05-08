package com.studypot.aistudyleader.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.identity.service.AuthSessionService;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootTest(
	classes = {AiStudyLeaderApplication.class, AuthSessionWiringTest.TestDataSourceConfiguration.class},
	properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
		"spring.datasource.url=jdbc:studypot-test",
		"studypot.auth.jwt.secret=0123456789abcdef0123456789abcdef",
		"studypot.auth.oauth2.backend-callback-uri=https://api.studypot.local/api/login/oauth2/code/google",
		"studypot.oauth.google.client-id=test-client-id",
		"studypot.oauth.google.client-secret=test-client-secret"
	}
)
class AuthSessionWiringTest {

	private final ApplicationContext context;

	@Autowired
	AuthSessionWiringTest(ApplicationContext context) {
		this.context = context;
	}

	@Test
	void authSessionServiceIsConfiguredWhenJdbcTemplateIsAutoConfigured() {
		assertThat(context.getBeanNamesForType(AuthSessionService.class))
			.containsExactly("authSessionService");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestDataSourceConfiguration {

		@Bean
		DataSource dataSource() {
			return mock(DataSource.class);
		}
	}
}
