package com.studypot.aistudyleader.studygroup.repository;

import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberSummary;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupJoinTarget;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupMemberProfile;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudyGroupRepository {

	void saveCreatedGroup(StudyGroup group, GroupMember ownerMember);

	boolean existsStudyGroup(UUID groupId);

	Optional<StudyGroup> findGroupByIdForMemberUserId(UUID groupId, UUID userId);

	Optional<StudyGroupJoinTarget> findJoinTargetByIdForUpdate(UUID groupId);

	boolean existsActiveOrOnboardingMember(UUID groupId, UUID userId);

	int countActiveOrOnboardingMembers(UUID groupId);

	void saveJoinedMember(GroupMember member);

	List<StudyGroup> findGroupsByMemberUserId(UUID userId);

	Optional<StudyGroupMemberProfile> findMyGroupMemberProfile(UUID groupId, UUID userId);

	List<GroupMemberSummary> findGroupMembers(UUID groupId);

	boolean updateMyGroupMemberDisplayName(UUID groupId, UUID userId, String displayName, Instant updatedAt);
}
