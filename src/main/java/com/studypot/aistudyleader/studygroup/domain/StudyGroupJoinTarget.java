package com.studypot.aistudyleader.studygroup.domain;

import java.util.Objects;
import java.util.UUID;

public record StudyGroupJoinTarget(
	UUID id,
	StudyGroupStatus status,
	int maxMembers,
	String inviteCode
) {

	private static final int MAX_INVITE_CODE_LENGTH = 80;

	public StudyGroupJoinTarget {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(status, "status must not be null");
		if (maxMembers <= 0) {
			throw new IllegalArgumentException("maxMembers must be positive");
		}
		inviteCode = requireText(inviteCode, "inviteCode", MAX_INVITE_CODE_LENGTH);
	}

	public boolean isAcceptingJoins() {
		return status == StudyGroupStatus.ONBOARDING
			|| status == StudyGroupStatus.READY_TO_START
			|| status == StudyGroupStatus.ACTIVE;
	}

	public boolean matchesInviteCode(String candidateInviteCode) {
		if (candidateInviteCode == null || candidateInviteCode.isBlank()) {
			return false;
		}
		return inviteCode.equals(candidateInviteCode.strip());
	}

	public boolean hasCapacity(int currentMemberCount) {
		if (currentMemberCount < 0) {
			throw new IllegalArgumentException("currentMemberCount must not be negative");
		}
		return currentMemberCount < maxMembers;
	}

	private static String requireText(String value, String fieldName, int maxLength) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		String normalized = value.strip();
		if (normalized.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " length must be <= " + maxLength + ": " + normalized.length());
		}
		return normalized;
	}
}
