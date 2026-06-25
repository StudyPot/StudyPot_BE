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
	void structuredOutputPurposesUseLowReasoningToPreserveBudget() {
		// 구조화 출력(JSON)을 쓰는 대화/회고 계열은 추론 모델이 토큰 예산을 추론에 소진해 content 가
		// 비는 실패가 잦으므로 low 로 고정한다.
		OpenAiReasoningEfforts efforts = OpenAiReasoningEfforts.defaults();

		assertThat(efforts.forPurpose(LlmUsagePurpose.TEAM_LEAD_CHAT)).isEqualTo(OpenAiReasoningEffort.LOW);
		assertThat(efforts.forPurpose(LlmUsagePurpose.RETROSPECTIVE_FEEDBACK)).isEqualTo(OpenAiReasoningEffort.LOW);
		assertThat(efforts.forPurpose(LlmUsagePurpose.RETROSPECTIVE_ANALYZE)).isEqualTo(OpenAiReasoningEffort.LOW);
		assertThat(efforts.forPurpose(LlmUsagePurpose.NEXT_WEEK_ADJUST)).isEqualTo(OpenAiReasoningEffort.LOW);
	}

	@Test
	void unconfiguredPurposesFallBackToModelDefault() {
		// 커리큘럼 생성은 별도 설정이 없어 모델 기본 추론(null)을 사용한다.
		assertThat(OpenAiReasoningEfforts.defaults().forPurpose(LlmUsagePurpose.CURRICULUM_GENERATE)).isNull();
	}

	@Test
	void studyRecommendationUsesConfiguredValue() {
		OpenAiReasoningEfforts efforts = new OpenAiReasoningEfforts(null, OpenAiReasoningEffort.LOW);

		assertThat(efforts.forPurpose(LlmUsagePurpose.STUDY_RECOMMENDATION)).isEqualTo(OpenAiReasoningEffort.LOW);
	}
}
