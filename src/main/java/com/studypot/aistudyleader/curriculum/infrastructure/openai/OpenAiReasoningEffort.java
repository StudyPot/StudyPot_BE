package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import java.util.Locale;

enum OpenAiReasoningEffort {
	MINIMAL,
	LOW,
	MEDIUM,
	HIGH;

	String apiValue() {
		return name().toLowerCase(Locale.ROOT);
	}
}
