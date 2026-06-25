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
			// 주차 리포트는 요약 작업이라 깊은 추론이 불필요하다. 추론(gpt-5 계열) 모델이 출력 토큰 예산을
			// 추론에 모두 소모해 message content 가 비는 문제(리포트 미생성)를 막기 위해 minimal 로 고정한다.
			case WEEKLY_REPORT -> OpenAiReasoningEffort.MINIMAL;
			default -> null;
		};
	}
}
