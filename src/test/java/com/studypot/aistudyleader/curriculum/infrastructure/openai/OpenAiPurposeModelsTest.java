package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import org.junit.jupiter.api.Test;

class OpenAiPurposeModelsTest {

	private static final String DEFAULT = "gpt-5-nano";

	@Test
	void emptySlotsFallBackToDefaultModel() {
		OpenAiPurposeModels models = OpenAiPurposeModels.none();

		for (LlmUsagePurpose purpose : LlmUsagePurpose.values()) {
			assertThat(models.modelFor(purpose, DEFAULT)).isEqualTo(DEFAULT);
		}
	}

	@Test
	void configuredSlotsOverridePerPurposeGroup() {
		OpenAiPurposeModels models = new OpenAiPurposeModels(
			"gpt-5-mini",  // detailKeywordSuggest
			"gpt-5.2",     // curriculumGenerate
			"gpt-5",       // retrospectiveFeedback
			"gpt-4o-mini", // teamLeadChat
			"gpt-5"        // studyRecommendation
		);

		assertThat(models.modelFor(LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST, DEFAULT)).isEqualTo("gpt-5-mini");
		assertThat(models.modelFor(LlmUsagePurpose.STUDY_RECOMMENDATION, DEFAULT)).isEqualTo("gpt-5");
		assertThat(models.modelFor(LlmUsagePurpose.CURRICULUM_GENERATE, DEFAULT)).isEqualTo("gpt-5.2");
		assertThat(models.modelFor(LlmUsagePurpose.CURRICULUM_REGENERATE_WEEK, DEFAULT)).isEqualTo("gpt-5.2");
		assertThat(models.modelFor(LlmUsagePurpose.RETROSPECTIVE_FEEDBACK, DEFAULT)).isEqualTo("gpt-5");
		assertThat(models.modelFor(LlmUsagePurpose.WEEKLY_REPORT, DEFAULT)).isEqualTo("gpt-5");
		assertThat(models.modelFor(LlmUsagePurpose.TEAM_LEAD_CHAT, DEFAULT)).isEqualTo("gpt-4o-mini");
	}

	@Test
	void blankSlotsAreTreatedAsUnset() {
		OpenAiPurposeModels models = new OpenAiPurposeModels("  ", "", null, "gpt-5-mini", "  ");

		assertThat(models.modelFor(LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST, DEFAULT)).isEqualTo(DEFAULT);
		assertThat(models.modelFor(LlmUsagePurpose.STUDY_RECOMMENDATION, DEFAULT)).isEqualTo(DEFAULT);
		assertThat(models.modelFor(LlmUsagePurpose.CURRICULUM_GENERATE, DEFAULT)).isEqualTo(DEFAULT);
		assertThat(models.modelFor(LlmUsagePurpose.TEAM_LEAD_CHAT, DEFAULT)).isEqualTo("gpt-5-mini");
	}
}
