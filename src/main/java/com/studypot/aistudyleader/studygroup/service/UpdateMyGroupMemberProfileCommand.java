package com.studypot.aistudyleader.studygroup.service;

import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import java.util.Objects;
import java.util.UUID;

public record UpdateMyGroupMemberProfileCommand(
	UUID authenticatedUserId,
	UUID groupId,
	String displayName
) {

	public UpdateMyGroupMemberProfileCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		try {
			displayName = GroupMember.normalizeDisplayName(displayName);
		} catch (IllegalArgumentException exception) {
			throw new InvalidStudyGroupMemberProfileRequestException("displayName", exception.getMessage());
		}
		if (displayName == null) {
			throw new InvalidStudyGroupMemberProfileRequestException("displayName", "displayName must not be blank.");
		}
	}
}
