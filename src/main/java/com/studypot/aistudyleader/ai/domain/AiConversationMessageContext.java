package com.studypot.aistudyleader.ai.domain;

import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.util.Objects;
import java.util.UUID;

public record AiConversationMessageContext(
	UUID conversationId,
	UUID groupId,
	UUID memberId,
	AiConversationStatus conversationStatus,
	StudyGroupStatus groupStatus,
	GroupMemberStatus memberStatus
) {

	public AiConversationMessageContext {
		Objects.requireNonNull(conversationId, "conversationId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(conversationStatus, "conversationStatus must not be null");
		Objects.requireNonNull(groupStatus, "groupStatus must not be null");
		Objects.requireNonNull(memberStatus, "memberStatus must not be null");
	}

	public boolean hasActiveMembership() {
		return groupStatus == StudyGroupStatus.ACTIVE && memberStatus == GroupMemberStatus.ACTIVE;
	}

	public boolean isOpen() {
		return conversationStatus == AiConversationStatus.OPEN;
	}
}
