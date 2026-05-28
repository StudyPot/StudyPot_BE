package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;

record OpenAiReasoningEfforts(OpenAiReasoningEffort detailKeywordSuggest) {

	OpenAiReasoningEfforts {
		detailKeywordSuggest = detailKeywordSuggest == null ? OpenAiReasoningEffort.MINIMAL : detailKeywordSuggest;
	}

	static OpenAiReasoningEfforts defaults() {
		return new OpenAiReasoningEfforts(OpenAiReasoningEffort.MINIMAL);
	}

	OpenAiReasoningEffort forPurpose(LlmUsagePurpose purpose) {
		return purpose == LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST ? detailKeywordSuggest : null;
	}
}
