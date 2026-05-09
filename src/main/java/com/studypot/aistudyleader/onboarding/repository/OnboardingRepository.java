package com.studypot.aistudyleader.onboarding.repository;

import com.studypot.aistudyleader.onboarding.domain.GroupOnboardingResponse;
import com.studypot.aistudyleader.onboarding.domain.OnboardingMemberContext;
import java.util.Optional;
import java.util.UUID;

public interface OnboardingRepository {

	boolean existsStudyGroup(UUID groupId);

	Optional<OnboardingMemberContext> findMemberContext(UUID groupId, UUID userId);

	Optional<GroupOnboardingResponse> findResponseByMemberId(UUID memberId);

	GroupOnboardingResponse saveDraft(GroupOnboardingResponse response);
}
