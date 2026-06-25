package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import org.junit.jupiter.api.Test;

class OpenAiReasoningEffortsTest {

	@Test
	void weeklyReportUsesLowReasoning() {
		// 주차 리포트는 품질을 위해 약간의 추론(low)을 유지하고, 넉넉한 출력 토큰 예산으로 본문 잘림을 막는다.
		OpenAiReasoningEfforts efforts = OpenAiReasoningEfforts.defaults();

		assertThat(efforts.forPurpose(LlmUsagePurpose.WEEKLY_REPORT)).isEqualTo(OpenAiReasoningEffort.LOW);
	}

	@Test
	void detailKeywordSuggestDefaultsToMinimal() {
		assertThat(OpenAiReasoningEfforts.defaults().forPurpose(LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST))
			.isEqualTo(OpenAiReasoningEffort.MINIMAL);
	}

	@Test
	void unconfiguredPurposesFallBackToModelDefault() {
		assertThat(OpenAiReasoningEfforts.defaults().forPurpose(LlmUsagePurpose.TEAM_LEAD_CHAT)).isNull();
	}

	@Test
	void studyRecommendationUsesConfiguredValue() {
		OpenAiReasoningEfforts efforts = new OpenAiReasoningEfforts(null, OpenAiReasoningEffort.LOW);

		assertThat(efforts.forPurpose(LlmUsagePurpose.STUDY_RECOMMENDATION)).isEqualTo(OpenAiReasoningEffort.LOW);
	}
}
