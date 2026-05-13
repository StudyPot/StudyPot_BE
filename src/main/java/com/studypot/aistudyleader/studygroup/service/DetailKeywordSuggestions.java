package com.studypot.aistudyleader.studygroup.service;

import java.util.List;
import java.util.Objects;

public record DetailKeywordSuggestions(List<DetailKeywordSuggestion> suggestions, String rationale) {

	public DetailKeywordSuggestions {
		suggestions = List.copyOf(Objects.requireNonNull(suggestions, "suggestions must not be null"));
		if (suggestions.isEmpty()) {
			throw new IllegalArgumentException("suggestions must not be empty");
		}
		rationale = rationale == null || rationale.isBlank() ? null : rationale.strip();
	}
}
