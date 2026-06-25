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
			// 주차 리포트는 품질을 위해 약간의 추론을 유지하되(low), 출력 토큰 예산을 넉넉히(weeklyReport=8192)
			// 줘서 추론이 예산을 소모해도 본문이 잘리지 않게 한다. (minimal 대비 요약 품질 ↑)
			case WEEKLY_REPORT -> OpenAiReasoningEffort.LOW;
			default -> null;
		};
	}
}
