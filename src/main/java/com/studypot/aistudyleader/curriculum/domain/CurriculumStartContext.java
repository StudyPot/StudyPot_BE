package com.studypot.aistudyleader.curriculum.domain;

import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CurriculumStartContext(
	UUID groupId,
	String groupName,
	String topic,
	List<String> detailKeywords,
	StudyGroupStatus groupStatus,
	LocalDate startsAt,
	LocalDate endsAt,
	UUID memberId,
	GroupMemberPermission permission,
	GroupMemberStatus memberStatus
) {

	public CurriculumStartContext {
		Objects.requireNonNull(groupId, "groupId must not be null");
		groupName = requireText(groupName, "groupName");
		topic = requireText(topic, "topic");
		detailKeywords = List.copyOf(Objects.requireNonNull(detailKeywords, "detailKeywords must not be null"));
		Objects.requireNonNull(groupStatus, "groupStatus must not be null");
		Objects.requireNonNull(startsAt, "startsAt must not be null");
		Objects.requireNonNull(endsAt, "endsAt must not be null");
		if (endsAt.isBefore(startsAt)) {
			throw new IllegalArgumentException("endsAt must be on or after startsAt");
		}
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(permission, "permission must not be null");
		Objects.requireNonNull(memberStatus, "memberStatus must not be null");
	}

	public boolean isOwner() {
		return permission == GroupMemberPermission.OWNER;
	}

	public boolean canReadCurriculum() {
		return isOwner() || memberStatus == GroupMemberStatus.ACTIVE;
	}

	public boolean hasActiveMembership() {
		return memberStatus == GroupMemberStatus.ACTIVE;
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.strip();
	}
}
