package com.studypot.aistudyleader.studygroup.service;

public record DetailKeywordSuggestion(String keyword, String reason) {

	public DetailKeywordSuggestion {
		keyword = requireText(keyword, "keyword");
		reason = reason == null || reason.isBlank() ? null : reason.strip();
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.strip();
	}
}
