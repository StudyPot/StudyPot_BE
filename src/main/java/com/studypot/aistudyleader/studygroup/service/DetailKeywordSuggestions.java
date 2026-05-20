package com.studypot.aistudyleader.studygroup.service;

import java.util.List;
import java.util.Objects;

public record DetailKeywordSuggestions(List<String> keywords) {

	public DetailKeywordSuggestions {
		keywords = List.copyOf(Objects.requireNonNull(keywords, "keywords must not be null").stream()
			.map(DetailKeywordSuggestions::requireKeyword)
			.distinct()
			.toList());
		if (keywords.isEmpty()) {
			throw new IllegalArgumentException("keywords must not be empty");
		}
	}

	private static String requireKeyword(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			throw new IllegalArgumentException("keywords must not contain blank values");
		}
		return keyword.strip();
	}
}
