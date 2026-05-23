package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;

record OpenAiOutputTokenLimits(
	Integer detailKeywordSuggest,
	Integer curriculumGenerate,
	Integer retrospectiveFeedback,
	Integer teamLeadChat
) {

	private static final int DEFAULT_DETAIL_KEYWORD_SUGGEST = 256;
	private static final int DEFAULT_CURRICULUM_GENERATE = 4096;
	private static final int DEFAULT_RETROSPECTIVE_FEEDBACK = 2048;
	private static final int DEFAULT_TEAM_LEAD_CHAT = 1536;

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
	}

	static OpenAiOutputTokenLimits defaults() {
		return new OpenAiOutputTokenLimits(null, null, null, null);
	}

	int forPurpose(LlmUsagePurpose purpose) {
		return switch (purpose) {
			case DETAIL_KEYWORD_SUGGEST -> detailKeywordSuggest;
			case CURRICULUM_GENERATE, CURRICULUM_REGENERATE_WEEK -> curriculumGenerate;
			case RETROSPECTIVE_ANALYZE, RETROSPECTIVE_FEEDBACK, NEXT_WEEK_ADJUST -> retrospectiveFeedback;
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
