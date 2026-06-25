package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import org.junit.jupiter.api.Test;

class OpenAiOutputTokenLimitsTest {

	@Test
	void weeklyReportUsesItsOwnGenerousBudgetByDefault() {
		// 주차 리포트는 전원 회고를 종합해 입력이 크고 추론 토큰을 소모하므로, 회고 피드백(2048)보다 넉넉한 전용 예산을 쓴다.
		OpenAiOutputTokenLimits limits = OpenAiOutputTokenLimits.defaults();

		assertThat(limits.forPurpose(LlmUsagePurpose.WEEKLY_REPORT)).isEqualTo(8192);
		assertThat(limits.forPurpose(LlmUsagePurpose.RETROSPECTIVE_FEEDBACK)).isEqualTo(2048);
	}

	@Test
	void weeklyReportBudgetIsConfigurableAndIndependentOfRetrospectiveFeedback() {
		OpenAiOutputTokenLimits limits = new OpenAiOutputTokenLimits(null, null, 2048, null, null, 6000);

		assertThat(limits.forPurpose(LlmUsagePurpose.WEEKLY_REPORT)).isEqualTo(6000);
		assertThat(limits.forPurpose(LlmUsagePurpose.RETROSPECTIVE_FEEDBACK)).isEqualTo(2048);
	}

	@Test
	void eachPurposeMapsToItsConfiguredLimit() {
		OpenAiOutputTokenLimits limits = new OpenAiOutputTokenLimits(11, 22, 33, 44, 55, 66);

		assertThat(limits.forPurpose(LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST)).isEqualTo(11);
		assertThat(limits.forPurpose(LlmUsagePurpose.CURRICULUM_GENERATE)).isEqualTo(22);
		assertThat(limits.forPurpose(LlmUsagePurpose.RETROSPECTIVE_FEEDBACK)).isEqualTo(33);
		assertThat(limits.forPurpose(LlmUsagePurpose.TEAM_LEAD_CHAT)).isEqualTo(44);
		assertThat(limits.forPurpose(LlmUsagePurpose.STUDY_RECOMMENDATION)).isEqualTo(55);
		assertThat(limits.forPurpose(LlmUsagePurpose.WEEKLY_REPORT)).isEqualTo(66);
	}
}
