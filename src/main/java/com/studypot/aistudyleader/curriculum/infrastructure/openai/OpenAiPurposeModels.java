package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;

/**
 * 용도(purpose)별 OpenAI 모델 매핑. 비어 있는 슬롯은 기본 모델(defaultModel)로 폴백한다.
 * 용도 그룹은 {@link OpenAiOutputTokenLimits}와 동일하게 묶는다.
 */
record OpenAiPurposeModels(
	String detailKeywordSuggest,
	String curriculumGenerate,
	String retrospectiveFeedback,
	String teamLeadChat
) {

	OpenAiPurposeModels {
		detailKeywordSuggest = blankToNull(detailKeywordSuggest);
		curriculumGenerate = blankToNull(curriculumGenerate);
		retrospectiveFeedback = blankToNull(retrospectiveFeedback);
		teamLeadChat = blankToNull(teamLeadChat);
	}

	static OpenAiPurposeModels none() {
		return new OpenAiPurposeModels(null, null, null, null);
	}

	String modelFor(LlmUsagePurpose purpose, String defaultModel) {
		String configured = switch (purpose) {
			case DETAIL_KEYWORD_SUGGEST -> detailKeywordSuggest;
			case CURRICULUM_GENERATE, CURRICULUM_REGENERATE_WEEK -> curriculumGenerate;
			case RETROSPECTIVE_ANALYZE, RETROSPECTIVE_FEEDBACK, NEXT_WEEK_ADJUST, WEEKLY_REPORT -> retrospectiveFeedback;
			case TEAM_LEAD_CHAT -> teamLeadChat;
		};
		return configured != null ? configured : defaultModel;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}
