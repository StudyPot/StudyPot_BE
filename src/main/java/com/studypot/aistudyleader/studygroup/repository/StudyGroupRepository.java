package com.studypot.aistudyleader.studygroup.repository;

import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupJoinTarget;
import java.util.Optional;
import java.util.UUID;

public interface StudyGroupRepository {

	void saveCreatedGroup(StudyGroup group, GroupMember ownerMember);

	Optional<StudyGroupJoinTarget> findJoinTargetByIdForUpdate(UUID groupId);

	boolean existsActiveOrOnboardingMember(UUID groupId, UUID userId);

	int countActiveOrOnboardingMembers(UUID groupId);

	void saveJoinedMember(GroupMember member);
}
