package com.studypot.aistudyleader.onboarding.repository;

import com.studypot.aistudyleader.onboarding.domain.GroupMemberOnboarding;
import com.studypot.aistudyleader.onboarding.domain.GroupOnboardingResponse;
import com.studypot.aistudyleader.onboarding.domain.OnboardingMemberContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OnboardingRepository {

	boolean existsStudyGroup(UUID groupId);

	Optional<OnboardingMemberContext> findMemberContext(UUID groupId, UUID userId);

	Optional<GroupOnboardingResponse> findResponseByMemberId(UUID memberId);

	GroupOnboardingResponse saveDraft(GroupOnboardingResponse response);

	GroupOnboardingResponse submit(GroupOnboardingResponse response);

	boolean activatePendingMember(UUID memberId, Instant activatedAt);

	boolean markStudyGroupReadyToStart(UUID groupId, Instant readyAt);

	List<GroupMemberOnboarding> findGroupOnboardings(UUID groupId);

	Optional<UUID> findOwnerUserIdWhenAllOnboarded(UUID groupId);

	List<UUID> findOtherMemberUserIds(UUID groupId, UUID excludeMemberId);
}
