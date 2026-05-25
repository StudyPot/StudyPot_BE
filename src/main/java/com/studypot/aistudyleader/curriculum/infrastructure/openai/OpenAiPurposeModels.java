package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;

record OpenAiPurposeModels(String detailKeywordSuggest) {

	OpenAiPurposeModels {
		detailKeywordSuggest = blankToNull(detailKeywordSuggest);
	}

	static OpenAiPurposeModels none() {
		return new OpenAiPurposeModels(null);
	}

	String modelFor(LlmUsagePurpose purpose, String defaultModel) {
		if (purpose == LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST && detailKeywordSuggest != null) {
			return detailKeywordSuggest;
		}
		return defaultModel;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}
