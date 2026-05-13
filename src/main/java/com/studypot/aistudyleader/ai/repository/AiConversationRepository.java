package com.studypot.aistudyleader.ai.repository;

import com.studypot.aistudyleader.ai.domain.AiConversation;
import com.studypot.aistudyleader.ai.domain.AiConversationMembershipContext;
import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageContext;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageCursor;
import com.studypot.aistudyleader.ai.domain.AiRetrospectiveReference;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiConversationRepository {

	boolean existsStudyGroup(UUID groupId);

	Optional<AiConversationMembershipContext> findMembership(UUID groupId, UUID userId);

	Optional<UUID> findWeekGroupId(UUID weekId);

	Optional<AiRetrospectiveReference> findRetrospectiveReference(UUID retrospectiveId);

	boolean insertConversation(AiConversation conversation);

	boolean existsConversation(UUID conversationId);

	Optional<AiConversationMessageContext> findMessageContext(UUID conversationId, UUID userId);

	boolean insertMessage(AiConversationMessage message);

	List<AiConversationMessage> findMessages(UUID conversationId, AiConversationMessageCursor cursor, int limit);
}
