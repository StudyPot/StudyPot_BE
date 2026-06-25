package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import org.junit.jupiter.api.Test;

class OpenAiReasoningEffortsTest {

	@Test
	void weeklyReportIsPinnedToMinimalReasoning() {
		// 주차 리포트는 추론 모델이 토큰 예산을 추론에 소진해 content 가 비는 문제를 막기 위해 minimal 로 고정한다.
		OpenAiReasoningEfforts efforts = OpenAiReasoningEfforts.defaults();

		assertThat(efforts.forPurpose(LlmUsagePurpose.WEEKLY_REPORT)).isEqualTo(OpenAiReasoningEffort.MINIMAL);
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
