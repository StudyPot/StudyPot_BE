package com.studypot.aistudyleader.onboarding.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record OnboardingMemberContext(
	UUID groupId,
	UUID memberId,
	List<String> detailKeywords
) {

	public OnboardingMemberContext {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		if (detailKeywords == null) {
			throw new IllegalArgumentException("detailKeywords must not be null");
		}
		detailKeywords = List.copyOf(detailKeywords.stream()
			.map(OnboardingMemberContext::requireKeyword)
			.toList());
	}

	public boolean containsKeyword(String keyword) {
		return keyword != null && detailKeywords.contains(keyword.strip());
	}

	private static String requireKeyword(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			throw new IllegalArgumentException("detailKeywords must not contain blank values");
		}
		return keyword.strip();
	}
}
