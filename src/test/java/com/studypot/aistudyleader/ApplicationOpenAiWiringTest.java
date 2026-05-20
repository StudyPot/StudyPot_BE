package com.studypot.aistudyleader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.ai.service.AiConversationAssistantResponseGenerator;
import com.studypot.aistudyleader.curriculum.service.CurriculumGenerator;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.studygroup.service.DetailKeywordSuggestionService;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootTest(
	classes = {AiStudyLeaderApplication.class, ApplicationOpenAiWiringTest.TestDataSourceConfiguration.class},
	properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
		"spring.datasource.url=jdbc:studypot-test",
		"studypot.auth.oauth2.backend-callback-uri=https://api.studypot.local/api/login/oauth2/code/google",
		"studypot.oauth.google.client-id=test-client-id",
		"studypot.oauth.google.client-secret=test-client-secret",
		"studypot.ai.openai.api-key=test-openai-key"
	}
)
class ApplicationOpenAiWiringTest {

	private final ApplicationContext context;

	@Autowired
	ApplicationOpenAiWiringTest(ApplicationContext context) {
		this.context = context;
	}

	@Test
	void openAiProviderAndProviderBackedAiGeneratorsAreConfiguredWhenApiKeyIsPresent() {
		assertThat(context.getBeanNamesForType(ObjectMapper.class))
			.isNotEmpty();
		assertThat(context.getBeanNamesForType(LlmProviderClient.class))
			.containsExactly("openAiLlmProvider");
		assertThat(context.getBeanNamesForType(CurriculumGenerator.class))
			.containsExactly("providerBackedCurriculumGenerator");
		assertThat(context.getBeanNamesForType(AiConversationAssistantResponseGenerator.class))
			.containsExactly("aiConversationAssistantResponseGenerator");
		assertThat(context.getBeanNamesForType(DetailKeywordSuggestionService.class))
			.containsExactly("detailKeywordSuggestionService");
		assertThat(context.containsBean("retrospectiveFeedbackGenerator"))
			.isTrue();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestDataSourceConfiguration {

		@Bean
		DataSource dataSource() {
			return mock(DataSource.class);
		}
	}
}
