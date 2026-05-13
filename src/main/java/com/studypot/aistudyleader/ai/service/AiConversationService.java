package com.studypot.aistudyleader.ai.service;

import com.studypot.aistudyleader.ai.domain.AiConversation;
import com.studypot.aistudyleader.ai.domain.AiConversationMembershipContext;
import com.studypot.aistudyleader.ai.domain.AiConversationType;
import com.studypot.aistudyleader.ai.domain.AiRetrospectiveReference;
import com.studypot.aistudyleader.ai.repository.AiConversationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.annotation.Transactional;

public class AiConversationService {

	private final AiConversationRepository repository;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;

	public AiConversationService(AiConversationRepository repository, Clock clock, Supplier<UUID> idGenerator) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
	}

	@Transactional
	public AiConversation openConversation(OpenAiConversationCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		AiConversationMembershipContext context = requireMembership(command.groupId(), command.authenticatedUserId());
		if (!context.canOpenConversation()) {
			throw new AiConversationAccessDeniedException("active group membership is required to open an AI conversation.");
		}

		UUID effectiveWeekId = validateWeek(command.groupId(), command.weekId());
		if (command.conversationType() != AiConversationType.RETROSPECTIVE && command.retrospectiveId() != null) {
			throw new InvalidAiConversationRequestException("retrospectiveId", "retrospectiveId is only allowed for RETROSPECTIVE conversations.");
		}
		if (command.retrospectiveId() != null) {
			AiRetrospectiveReference reference = requireRetrospectiveReference(command.retrospectiveId());
			validateRetrospectiveReference(command.groupId(), context.memberId(), effectiveWeekId, reference);
			effectiveWeekId = reference.curriculumWeekId();
		}

		Instant now = clock.instant();
		AiConversation conversation = AiConversation.open(
			idGenerator.get(),
			command.groupId(),
			context.memberId(),
			effectiveWeekId,
			command.retrospectiveId(),
			command.conversationType(),
			now
		);
		if (!repository.insertConversation(conversation)) {
			throw new AiConversationMutationRejectedException("AI conversation could not be inserted.");
		}
		return conversation;
	}

	private AiConversationMembershipContext requireMembership(UUID groupId, UUID userId) {
		return repository.findMembership(groupId, userId)
			.orElseGet(() -> {
				if (!repository.existsStudyGroup(groupId)) {
					throw new AiConversationNotFoundException("study group was not found.");
				}
				throw new AiConversationAccessDeniedException("authenticated user is not a member of this study group.");
			});
	}

	private UUID validateWeek(UUID groupId, UUID weekId) {
		if (weekId == null) {
			return null;
		}
		UUID weekGroupId = repository.findWeekGroupId(weekId)
			.orElseThrow(() -> new AiConversationNotFoundException("curriculum week was not found."));
		if (!groupId.equals(weekGroupId)) {
			throw new AiConversationAccessDeniedException("curriculum week does not belong to this study group.");
		}
		return weekId;
	}

	private AiRetrospectiveReference requireRetrospectiveReference(UUID retrospectiveId) {
		return repository.findRetrospectiveReference(retrospectiveId)
			.orElseThrow(() -> new AiConversationNotFoundException("retrospective was not found."));
	}

	private static void validateRetrospectiveReference(
		UUID groupId,
		UUID memberId,
		UUID weekId,
		AiRetrospectiveReference reference
	) {
		if (!groupId.equals(reference.groupId())) {
			throw new AiConversationAccessDeniedException("retrospective does not belong to this study group.");
		}
		if (!memberId.equals(reference.memberId())) {
			throw new AiConversationAccessDeniedException("retrospective does not belong to the authenticated member.");
		}
		if (weekId != null && !weekId.equals(reference.curriculumWeekId())) {
			throw new InvalidAiConversationRequestException("retrospectiveId", "retrospective must belong to the requested week.");
		}
	}
}
