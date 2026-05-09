package com.studypot.aistudyleader.studygroup.service;

import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import java.util.Objects;

public record StudyGroupJoinResult(GroupMember member) {

	public StudyGroupJoinResult {
		Objects.requireNonNull(member, "member must not be null");
	}
}
