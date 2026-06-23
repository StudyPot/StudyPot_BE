package com.studypot.aistudyleader.studygroup.repository;

import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberSummary;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupJoinTarget;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupMemberProfile;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface StudyGroupRepository {

	void saveCreatedGroup(StudyGroup group, GroupMember ownerMember);

	boolean existsStudyGroup(UUID groupId);

	Optional<StudyGroup> findGroupByIdForMemberUserId(UUID groupId, UUID userId);

	Optional<StudyGroupJoinTarget> findJoinTargetByIdForUpdate(UUID groupId);

	Optional<StudyGroupJoinTarget> findJoinTargetByInviteCode(String inviteCode);

	Optional<UUID> findOwnerUserId(UUID groupId);

	boolean revertReadyToStartToOnboarding(UUID groupId, Instant updatedAt);

	boolean existsActiveOrOnboardingMember(UUID groupId, UUID userId);

	int countActiveOrOnboardingMembers(UUID groupId);

	Map<UUID, Integer> countActiveOrOnboardingMembersByGroupIds(Collection<UUID> groupIds);

	void saveJoinedMember(GroupMember member);

	List<StudyGroup> findGroupsByMemberUserId(UUID userId);

	Optional<StudyGroupMemberProfile> findMyGroupMemberProfile(UUID groupId, UUID userId);

	List<GroupMemberSummary> findGroupMembers(UUID groupId);

	boolean updateMyGroupMemberDisplayName(UUID groupId, UUID userId, String displayName, Instant updatedAt);

	boolean softDeleteGroup(UUID groupId, Instant deletedAt);

	boolean updateGroup(StudyGroup group);
}
