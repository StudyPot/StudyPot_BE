package com.studypot.aistudyleader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.studypot.aistudyleader.ai.repository.AiConversationRepository;
import com.studypot.aistudyleader.ai.service.AiConversationService;
import com.studypot.aistudyleader.curriculum.repository.CurriculumRepository;
import com.studypot.aistudyleader.curriculum.service.CurriculumGenerator;
import com.studypot.aistudyleader.curriculum.service.CurriculumService;
import com.studypot.aistudyleader.llm.repository.LlmUsageRepository;
import com.studypot.aistudyleader.llm.service.LlmUsageService;
import com.studypot.aistudyleader.onboarding.repository.OnboardingRepository;
import com.studypot.aistudyleader.onboarding.service.OnboardingService;
import com.studypot.aistudyleader.retrospective.repository.RetrospectiveRepository;
import com.studypot.aistudyleader.retrospective.service.RetrospectiveService;
import com.studypot.aistudyleader.studygroup.rules.repository.GroupRuleRepository;
import com.studypot.aistudyleader.studygroup.rules.service.GroupRuleService;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootTest(
	classes = {AiStudyLeaderApplication.class, ApplicationFeatureWiringTest.TestDataSourceConfiguration.class},
	properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
		"spring.datasource.url=jdbc:studypot-test",
		"studypot.auth.oauth2.backend-callback-uri=https://api.studypot.local/api/login/oauth2/code/google",
		"studypot.oauth.google.client-id=test-client-id",
		"studypot.oauth.google.client-secret=test-client-secret",
		"studypot.ai.openai.api-key="
	}
)
class ApplicationFeatureWiringTest {

	private final ApplicationContext context;

	@Autowired
	ApplicationFeatureWiringTest(ApplicationContext context) {
		this.context = context;
	}

	@Test
	void featureRepositoriesAreConfiguredWhenJdbcTemplateIsAutoConfigured() {
		assertThat(context.getBeanNamesForType(OnboardingRepository.class))
			.containsExactly("onboardingRepository");
		assertThat(context.getBeanNamesForType(CurriculumRepository.class))
			.containsExactly("curriculumRepository");
		assertThat(context.getBeanNamesForType(GroupRuleRepository.class))
			.containsExactly("groupRuleRepository");
		assertThat(context.getBeanNamesForType(RetrospectiveRepository.class))
			.containsExactly("retrospectiveRepository");
		assertThat(context.getBeanNamesForType(AiConversationRepository.class))
			.containsExactly("aiConversationRepository");
		assertThat(context.getBeanNamesForType(LlmUsageRepository.class))
			.containsExactly("llmUsageRepository");
	}

	@Test
	void weeklyTodoRuleRetrospectiveAndAiServicesAreConfiguredWithoutOpenAiGenerator() {
		assertThat(context.getBeanNamesForType(CurriculumGenerator.class))
			.isEmpty();
		assertThat(context.getBeanNamesForType(OnboardingService.class))
			.containsExactly("onboardingService");
		assertThat(context.getBeanNamesForType(CurriculumService.class))
			.containsExactly("curriculumService");
		assertThat(context.getBeanNamesForType(GroupRuleService.class))
			.containsExactly("groupRuleService");
		assertThat(context.getBeanNamesForType(RetrospectiveService.class))
			.containsExactly("retrospectiveService");
		assertThat(context.getBeanNamesForType(AiConversationService.class))
			.containsExactly("aiConversationService");
		assertThat(context.getBeanNamesForType(LlmUsageService.class))
			.containsExactly("llmUsageService");
	}

	@Test
	void notificationEventPublisherResolvesToInProcessServiceWhenRabbitDisabled() {
		// RabbitMQ 비활성(프로덕션 현 상태) 시, 도메인 서비스가 주입받는 NotificationEventPublisher가
		// noop이 아니라 실제 발행자(NotificationService, in-process)로 해석되는지 검증한다.
		com.studypot.aistudyleader.notification.service.NotificationEventPublisher resolved =
			context.getBeanProvider(com.studypot.aistudyleader.notification.service.NotificationEventPublisher.class)
				.getIfAvailable();
		assertThat(resolved)
			.isInstanceOf(com.studypot.aistudyleader.notification.service.NotificationService.class);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestDataSourceConfiguration {

		@Bean
		DataSource dataSource() {
			return mock(DataSource.class);
		}
	}
}
