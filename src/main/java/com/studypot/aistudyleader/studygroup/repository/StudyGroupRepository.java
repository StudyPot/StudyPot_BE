package com.studypot.aistudyleader.studygroup.repository;

import com.studypot.aistudyleader.studygroup.domain.AiManagerView;
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

	/** 호스트(created_by)로서 동시에 운영 중(미완료)인 스터디 개수. */
	int countActiveHostedGroups(UUID hostUserId);

	/** 사용자 플랜('FREE'/'PREMIUM'). 행이 없으면 'FREE'. */
	String findUserPlan(UUID userId);

	Optional<AiManagerView> findAiManager(UUID groupId);

	boolean updateAiManager(UUID groupId, String persona, UUID updatedBy, Instant updatedAt);

	void saveJoinedMember(GroupMember member);

	List<StudyGroup> findGroupsByMemberUserId(UUID userId);

	Optional<StudyGroupMemberProfile> findMyGroupMemberProfile(UUID groupId, UUID userId);

	List<GroupMemberSummary> findGroupMembers(UUID groupId);

	boolean updateMyGroupMemberDisplayName(UUID groupId, UUID userId, String displayName, Instant updatedAt);

	boolean softDeleteGroup(UUID groupId, Instant deletedAt);

	boolean updateGroup(StudyGroup group);
}
