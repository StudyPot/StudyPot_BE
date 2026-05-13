package com.studypot.aistudyleader.studygroup.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SuggestDetailKeywordsCommand(
	UUID authenticatedUserId,
	String topic,
	List<String> hintKeywords,
	int maxCandidates
) {

	private static final int MAX_CANDIDATES_LIMIT = 10;

	public SuggestDetailKeywordsCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		topic = requireText(topic, "topic");
		hintKeywords = List.copyOf(Objects.requireNonNull(hintKeywords, "hintKeywords must not be null").stream()
			.map(value -> requireText(value, "hintKeyword"))
			.distinct()
			.toList());
		if (maxCandidates <= 0 || maxCandidates > MAX_CANDIDATES_LIMIT) {
			throw new IllegalArgumentException("maxCandidates must be between 1 and 10");
		}
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.strip();
	}
}
