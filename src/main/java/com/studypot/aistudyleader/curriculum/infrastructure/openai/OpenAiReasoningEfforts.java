package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;

record OpenAiReasoningEfforts(
	OpenAiReasoningEffort detailKeywordSuggest,
	OpenAiReasoningEffort studyRecommendation
) {

	OpenAiReasoningEfforts {
		detailKeywordSuggest = detailKeywordSuggest == null ? OpenAiReasoningEffort.MINIMAL : detailKeywordSuggest;
		// 추천(studyRecommendation)은 비워두면(null) 모델 기본 추론을 사용한다. minimal 로 고정하지 않는다.
	}

	static OpenAiReasoningEfforts defaults() {
		return new OpenAiReasoningEfforts(OpenAiReasoningEffort.MINIMAL, null);
	}

	OpenAiReasoningEffort forPurpose(LlmUsagePurpose purpose) {
		return switch (purpose) {
			case DETAIL_KEYWORD_SUGGEST -> detailKeywordSuggest;
			case STUDY_RECOMMENDATION -> studyRecommendation;
			default -> null;
		};
	}
}
