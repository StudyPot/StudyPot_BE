package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;

record OpenAiOutputTokenLimits(
	Integer detailKeywordSuggest,
	Integer curriculumGenerate,
	Integer retrospectiveFeedback,
	Integer teamLeadChat,
	Integer studyRecommendation
) {

	private static final int DEFAULT_DETAIL_KEYWORD_SUGGEST = 256;
	private static final int DEFAULT_CURRICULUM_GENERATE = 16_384;
	private static final int DEFAULT_RETROSPECTIVE_FEEDBACK = 2048;
	// 추론(gpt-5 계열) 모델은 이 예산을 추론 토큰으로도 소모하므로, JSON 출력이 잘리지 않게 여유를 둔다.
	// (cap 상향이며 실제 과금은 사용한 토큰 기준이라 비용 영향은 없다.)
	private static final int DEFAULT_TEAM_LEAD_CHAT = 4096;
	// 추천 3건(제목+이유)을 추론 토큰까지 감안해 잘리지 않게 담을 여유.
	private static final int DEFAULT_STUDY_RECOMMENDATION = 2048;

	OpenAiOutputTokenLimits {
		detailKeywordSuggest = positiveOrDefault(
			detailKeywordSuggest,
			DEFAULT_DETAIL_KEYWORD_SUGGEST,
			"detailKeywordSuggest"
		);
		curriculumGenerate = positiveOrDefault(
			curriculumGenerate,
			DEFAULT_CURRICULUM_GENERATE,
			"curriculumGenerate"
		);
		retrospectiveFeedback = positiveOrDefault(
			retrospectiveFeedback,
			DEFAULT_RETROSPECTIVE_FEEDBACK,
			"retrospectiveFeedback"
		);
		teamLeadChat = positiveOrDefault(teamLeadChat, DEFAULT_TEAM_LEAD_CHAT, "teamLeadChat");
		studyRecommendation = positiveOrDefault(
			studyRecommendation,
			DEFAULT_STUDY_RECOMMENDATION,
			"studyRecommendation"
		);
	}

	static OpenAiOutputTokenLimits defaults() {
		return new OpenAiOutputTokenLimits(null, null, null, null, null);
	}

	int forPurpose(LlmUsagePurpose purpose) {
		return switch (purpose) {
			case DETAIL_KEYWORD_SUGGEST -> detailKeywordSuggest;
			case STUDY_RECOMMENDATION -> studyRecommendation;
			case CURRICULUM_GENERATE, CURRICULUM_REGENERATE_WEEK -> curriculumGenerate;
			case RETROSPECTIVE_ANALYZE, RETROSPECTIVE_FEEDBACK, NEXT_WEEK_ADJUST, WEEKLY_REPORT -> retrospectiveFeedback;
			case TEAM_LEAD_CHAT -> teamLeadChat;
		};
	}

	private static int positiveOrDefault(Integer value, int defaultValue, String fieldName) {
		int resolved = value == null ? defaultValue : value;
		if (resolved <= 0) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		return resolved;
	}
}
