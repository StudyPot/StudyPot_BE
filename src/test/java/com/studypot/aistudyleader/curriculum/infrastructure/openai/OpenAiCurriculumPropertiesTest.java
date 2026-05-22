package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class OpenAiCurriculumPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(PropertiesConfiguration.class);

	@Test
	void defaultsToResponsesMode() {
		contextRunner.run(context -> assertThat(context.getBean(OpenAiCurriculumProperties.class).apiMode())
			.isEqualTo(OpenAiApiMode.RESPONSES));
	}

	@Test
	void bindsChatCompletionsModeFromKebabCaseProperty() {
		contextRunner
			.withPropertyValues("studypot.ai.openai.api-mode=chat-completions")
			.run(context -> assertThat(context.getBean(OpenAiCurriculumProperties.class).apiMode())
				.isEqualTo(OpenAiApiMode.CHAT_COMPLETIONS));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(OpenAiCurriculumProperties.class)
	static class PropertiesConfiguration {
	}
}
