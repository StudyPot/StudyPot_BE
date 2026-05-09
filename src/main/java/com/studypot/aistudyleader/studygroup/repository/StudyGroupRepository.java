package com.studypot.aistudyleader.studygroup.repository;

import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;

public interface StudyGroupRepository {

	void saveCreatedGroup(StudyGroup group, GroupMember ownerMember);
}
