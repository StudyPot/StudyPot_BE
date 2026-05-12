package com.studypot.aistudyleader.onboarding.domain;

import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record OnboardingMemberContext(
	UUID groupId,
	UUID memberId,
	GroupMemberStatus memberStatus,
	List<String> detailKeywords
) {

	public OnboardingMemberContext {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(memberStatus, "memberStatus must not be null");
		detailKeywords = List.copyOf(Objects.requireNonNull(detailKeywords, "detailKeywords must not be null").stream()
			.map(OnboardingMemberContext::requireKeyword)
			.toList());
	}

	public OnboardingMemberContext(UUID groupId, UUID memberId, List<String> detailKeywords) {
		this(groupId, memberId, GroupMemberStatus.PENDING_ONBOARDING, detailKeywords);
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
