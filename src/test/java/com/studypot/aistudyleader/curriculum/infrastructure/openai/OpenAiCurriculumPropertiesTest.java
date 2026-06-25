package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import java.time.Duration;
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
	void defaultsReadTimeoutForLongStructuredGeneration() {
		contextRunner.run(context -> assertThat(context.getBean(OpenAiCurriculumProperties.class).readTimeout())
			.isEqualTo(Duration.ofSeconds(120)));
	}

	@Test
	void defaultsOutputTokenLimitsByPurpose() {
		contextRunner.run(context -> {
			OpenAiOutputTokenLimits limits = context.getBean(OpenAiCurriculumProperties.class).outputTokenLimits();

			assertThat(limits.detailKeywordSuggest()).isEqualTo(256);
			assertThat(limits.curriculumGenerate()).isEqualTo(16_384);
			assertThat(limits.retrospectiveFeedback()).isEqualTo(2048);
			assertThat(limits.teamLeadChat()).isEqualTo(4096);
			assertThat(limits.studyRecommendation()).isEqualTo(2048);
			assertThat(limits.forPurpose(LlmUsagePurpose.STUDY_RECOMMENDATION)).isEqualTo(2048);
		});
	}

	@Test
	void defaultsDetailKeywordReasoningEffortToMinimal() {
		contextRunner.run(context -> {
			OpenAiReasoningEfforts reasoningEfforts = context.getBean(OpenAiCurriculumProperties.class).reasoningEfforts();

			assertThat(reasoningEfforts.forPurpose(LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST))
				.isEqualTo(OpenAiReasoningEffort.MINIMAL);
			assertThat(reasoningEfforts.forPurpose(LlmUsagePurpose.CURRICULUM_GENERATE)).isNull();
			// 추천은 비워두면 모델 기본 추론(null)을 사용한다(minimal 로 고정하지 않음).
			assertThat(reasoningEfforts.forPurpose(LlmUsagePurpose.STUDY_RECOMMENDATION)).isNull();
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
				"studypot.ai.openai.output-token-limits.team-lead-chat=404",
				"studypot.ai.openai.output-token-limits.study-recommendation=505"
			)
			.run(context -> {
				OpenAiOutputTokenLimits limits = context.getBean(OpenAiCurriculumProperties.class).outputTokenLimits();

				assertThat(limits.detailKeywordSuggest()).isEqualTo(101);
				assertThat(limits.curriculumGenerate()).isEqualTo(202);
				assertThat(limits.retrospectiveFeedback()).isEqualTo(303);
				assertThat(limits.teamLeadChat()).isEqualTo(404);
				assertThat(limits.studyRecommendation()).isEqualTo(505);
			});
	}

	@Test
	void bindsPurposeSpecificModelOverridesFromKebabCaseProperties() {
		contextRunner
			.withPropertyValues(
				"studypot.ai.openai.model=gpt-5.2",
				"studypot.ai.openai.models.detail-keyword-suggest=gpt-5-nano",
				"studypot.ai.openai.models.study-recommendation=gpt-5-mini"
			)
			.run(context -> {
				OpenAiCurriculumProperties properties = context.getBean(OpenAiCurriculumProperties.class);

				assertThat(properties.models().modelFor(LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST, properties.model()))
					.isEqualTo("gpt-5-nano");
				assertThat(properties.models().modelFor(LlmUsagePurpose.STUDY_RECOMMENDATION, properties.model()))
					.isEqualTo("gpt-5-mini");
				assertThat(properties.models().modelFor(LlmUsagePurpose.CURRICULUM_GENERATE, properties.model()))
					.isEqualTo("gpt-5.2");
			});
	}

	@Test
	void bindsReasoningEffortsFromKebabCaseProperties() {
		contextRunner
			.withPropertyValues(
				"studypot.ai.openai.reasoning-efforts.detail-keyword-suggest=low",
				"studypot.ai.openai.reasoning-efforts.study-recommendation=medium"
			)
			.run(context -> {
				OpenAiReasoningEfforts reasoningEfforts =
					context.getBean(OpenAiCurriculumProperties.class).reasoningEfforts();

				assertThat(reasoningEfforts.forPurpose(LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST))
					.isEqualTo(OpenAiReasoningEffort.LOW);
				assertThat(reasoningEfforts.forPurpose(LlmUsagePurpose.STUDY_RECOMMENDATION))
					.isEqualTo(OpenAiReasoningEffort.MEDIUM);
			});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(OpenAiCurriculumProperties.class)
	static class PropertiesConfiguration {
	}
}
