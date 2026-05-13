package com.studypot.aistudyleader.ai.domain;

import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.util.Objects;
import java.util.UUID;

public record AiConversationMessageContext(
	UUID conversationId,
	UUID groupId,
	UUID memberId,
	UUID curriculumWeekId,
	UUID retrospectiveId,
	AiConversationType conversationType,
	String summary,
	AiConversationStatus conversationStatus,
	StudyGroupStatus groupStatus,
	GroupMemberStatus memberStatus
) {

	public AiConversationMessageContext {
		Objects.requireNonNull(conversationId, "conversationId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(conversationType, "conversationType must not be null");
		summary = summary == null ? "" : summary.strip();
		Objects.requireNonNull(conversationStatus, "conversationStatus must not be null");
		Objects.requireNonNull(groupStatus, "groupStatus must not be null");
		Objects.requireNonNull(memberStatus, "memberStatus must not be null");
	}

	public AiConversationMessageContext(
		UUID conversationId,
		UUID groupId,
		UUID memberId,
		AiConversationStatus conversationStatus,
		StudyGroupStatus groupStatus,
		GroupMemberStatus memberStatus
	) {
		this(
			conversationId,
			groupId,
			memberId,
			null,
			null,
			AiConversationType.TEAM_LEAD_CHAT,
			"",
			conversationStatus,
			groupStatus,
			memberStatus
		);
	}

	public boolean hasActiveMembership() {
		return groupStatus == StudyGroupStatus.ACTIVE && memberStatus == GroupMemberStatus.ACTIVE;
	}

	public boolean isOpen() {
		return conversationStatus == AiConversationStatus.OPEN;
	}
}
