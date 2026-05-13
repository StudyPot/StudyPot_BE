package com.studypot.aistudyleader.ai.repository;

import com.studypot.aistudyleader.ai.domain.AiConversation;
import com.studypot.aistudyleader.ai.domain.AiConversationMembershipContext;
import com.studypot.aistudyleader.ai.domain.AiRetrospectiveReference;
import java.util.Optional;
import java.util.UUID;

public interface AiConversationRepository {

	boolean existsStudyGroup(UUID groupId);

	Optional<AiConversationMembershipContext> findMembership(UUID groupId, UUID userId);

	Optional<UUID> findWeekGroupId(UUID weekId);

	Optional<AiRetrospectiveReference> findRetrospectiveReference(UUID retrospectiveId);

	boolean insertConversation(AiConversation conversation);
}
