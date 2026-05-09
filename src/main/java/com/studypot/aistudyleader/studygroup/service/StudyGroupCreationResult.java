package com.studypot.aistudyleader.studygroup.service;

import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import java.util.Objects;

public record StudyGroupCreationResult(StudyGroup group, GroupMember ownerMember) {

	public StudyGroupCreationResult {
		Objects.requireNonNull(group, "group must not be null");
		Objects.requireNonNull(ownerMember, "ownerMember must not be null");
	}
}
