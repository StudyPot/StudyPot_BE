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
	void defaultsOutputTokenLimitsByPurpose() {
		contextRunner.run(context -> {
			OpenAiOutputTokenLimits limits = context.getBean(OpenAiCurriculumProperties.class).outputTokenLimits();

			assertThat(limits.detailKeywordSuggest()).isEqualTo(256);
			assertThat(limits.curriculumGenerate()).isEqualTo(4096);
			assertThat(limits.retrospectiveFeedback()).isEqualTo(2048);
			assertThat(limits.teamLeadChat()).isEqualTo(1536);
		});
	}

	@Test
	void bindsChatCompletionsModeFromKebabCaseProperty() {
		contextRunner
			.withPropertyValues("studypot.ai.openai.api-mode=chat-completions")
			.run(context -> assertThat(context.getBean(OpenAiCurriculumProperties.class).apiMode())
					.isEqualTo(OpenAiApiMode.CHAT_COMPLETIONS));
	}

	@Test
	void bindsOutputTokenLimitsFromKebabCaseProperties() {
		contextRunner
			.withPropertyValues(
				"studypot.ai.openai.output-token-limits.detail-keyword-suggest=101",
				"studypot.ai.openai.output-token-limits.curriculum-generate=202",
				"studypot.ai.openai.output-token-limits.retrospective-feedback=303",
				"studypot.ai.openai.output-token-limits.team-lead-chat=404"
			)
			.run(context -> {
				OpenAiOutputTokenLimits limits = context.getBean(OpenAiCurriculumProperties.class).outputTokenLimits();

				assertThat(limits.detailKeywordSuggest()).isEqualTo(101);
				assertThat(limits.curriculumGenerate()).isEqualTo(202);
				assertThat(limits.retrospectiveFeedback()).isEqualTo(303);
				assertThat(limits.teamLeadChat()).isEqualTo(404);
			});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(OpenAiCurriculumProperties.class)
	static class PropertiesConfiguration {
	}
}
