package com.studypot.aistudyleader.ai.service;

import com.studypot.aistudyleader.ai.domain.AiConversation;
import com.studypot.aistudyleader.ai.domain.AiConversationMembershipContext;
import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageContext;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageCursor;
import com.studypot.aistudyleader.ai.domain.AiConversationPromptContext;
import com.studypot.aistudyleader.ai.domain.AiConversationType;
import com.studypot.aistudyleader.ai.domain.AiRetrospectiveReference;
import com.studypot.aistudyleader.ai.repository.AiConversationRepository;
import com.studypot.aistudyleader.global.api.CursorPageResponse;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class AiConversationService {

	private static final Base64.Encoder CURSOR_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder CURSOR_DECODER = Base64.getUrlDecoder();
	private static final Logger log = LoggerFactory.getLogger(AiConversationService.class);

	private final AiConversationRepository repository;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;
	private final AiConversationAssistantResponseGenerator assistantResponseGenerator;
	private final LlmUsageRecorder usageRecorder;
	private final AiConversationStreamPublisher streamPublisher;

	public AiConversationService(AiConversationRepository repository, Clock clock, Supplier<UUID> idGenerator) {
		this(repository, clock, idGenerator, null, null);
	}

	public AiConversationService(
		AiConversationRepository repository,
		Clock clock,
		Supplier<UUID> idGenerator,
		AiConversationAssistantResponseGenerator assistantResponseGenerator,
		LlmUsageRecorder usageRecorder
	) {
		this(repository, clock, idGenerator, assistantResponseGenerator, usageRecorder, AiConversationStreamPublisher.noop());
	}

	public AiConversationService(
		AiConversationRepository repository,
		Clock clock,
		Supplier<UUID> idGenerator,
		AiConversationAssistantResponseGenerator assistantResponseGenerator,
		LlmUsageRecorder usageRecorder,
		AiConversationStreamPublisher streamPublisher
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
		this.assistantResponseGenerator = assistantResponseGenerator;
		this.usageRecorder = usageRecorder;
		this.streamPublisher = Objects.requireNonNull(streamPublisher, "streamPublisher must not be null");
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

	@Transactional(noRollbackFor = AiConversationResponseGenerationException.class)
	public AiConversationMessage sendMessage(SendAiConversationMessageCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		AiConversationMessageContext context = requireMessageContext(command.conversationId(), command.authenticatedUserId());
		if (!context.hasActiveMembership()) {
			throw new AiConversationAccessDeniedException("active group membership is required to send an AI conversation message.");
		}
		if (!context.isOpen()) {
			throw new AiConversationMutationRejectedException("AI conversation is closed.");
		}

		AiConversationMessage message = AiConversationMessage.userMessage(
			idGenerator.get(),
			command.conversationId(),
			command.content(),
			clock.instant()
		);
		if (!repository.insertMessage(message)) {
			throw new AiConversationMutationRejectedException("AI conversation message could not be inserted.");
		}
		publishStreamEventSafely(() -> streamPublisher.publishUserMessageSaved(message), "user-message-saved");
		if (!assistantGenerationConfigured()) {
			return message;
		}
		return generateAssistantMessage(command, context, message);
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<AiConversationMessage> listMessages(ListAiConversationMessagesQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		AiConversationMessageContext context = requireMessageContext(query.conversationId(), query.authenticatedUserId());
		if (!context.hasActiveMembership()) {
			throw new AiConversationAccessDeniedException("active group membership is required to read AI conversation messages.");
		}
		AiConversationMessageCursor cursor = decodeCursor(query.cursor());
		List<AiConversationMessage> fetched = repository.findMessages(query.conversationId(), cursor, query.pageSize() + 1);
		if (fetched.size() <= query.pageSize()) {
			return CursorPageResponse.firstPage(fetched, null);
		}
		List<AiConversationMessage> items = List.copyOf(fetched.subList(0, query.pageSize()));
		return CursorPageResponse.firstPage(items, encodeCursor(items.getLast()));
	}

	@Transactional(readOnly = true)
	public void validateConversationStreamAccess(SubscribeAiConversationStreamQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		AiConversationMessageContext context = requireMessageContext(query.conversationId(), query.authenticatedUserId());
		if (!context.hasActiveMembership()) {
			throw new AiConversationAccessDeniedException("active group membership is required to stream AI conversation messages.");
		}
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

	private AiConversationMessageContext requireMessageContext(UUID conversationId, UUID userId) {
		return repository.findMessageContext(conversationId, userId)
			.orElseGet(() -> {
				if (!repository.existsConversation(conversationId)) {
					throw new AiConversationNotFoundException("AI conversation was not found.");
				}
				throw new AiConversationAccessDeniedException("authenticated user is not a member of this AI conversation.");
			});
	}

	private AiConversationMessage generateAssistantMessage(
		SendAiConversationMessageCommand command,
		AiConversationMessageContext context,
		AiConversationMessage userMessage
	) {
		AiConversationPromptContext promptContext = repository.findPromptContext(context, 20);
		AiConversationAssistantResponse response;
		publishStreamEventSafely(() -> streamPublisher.publishAssistantGenerationStarted(context.conversationId()),
			"assistant-generation-started");
		try {
			response = assistantResponseGenerator.generate(new AiConversationAssistantRequest(
				command.authenticatedUserId(),
				context,
				userMessage,
				promptContext
			));
		} catch (AiConversationResponseGenerationException exception) {
			recordFailureUsage(command, context, exception);
			publishStreamEventSafely(
				() -> streamPublisher.publishAssistantGenerationFailed(context.conversationId(), exception.failure().errorCode()),
				"assistant-generation-failed"
			);
			throw exception;
		}

		Instant now = clock.instant();
		UUID usageId = idGenerator.get();
		usageRecorder.record(response.llmResponse().toUsage(
			usageId,
			command.authenticatedUserId(),
			context.groupId(),
			LlmUsagePurpose.TEAM_LEAD_CHAT,
			now
		));
		AiConversationMessage assistantMessage = AiConversationMessage.assistantMessage(
			idGenerator.get(),
			context.conversationId(),
			usageId,
			response.message(),
			assistantMetadata(response.metadata()),
			now
		);
		if (!repository.insertMessage(assistantMessage)) {
			throw new AiConversationMutationRejectedException("AI conversation assistant message could not be inserted.");
		}
		updateConversationSummaryIfNeeded(context, response.conversationSummaryPatch(), now);
		publishStreamEventSafely(() -> streamPublisher.publishAssistantMessageCreated(assistantMessage), "assistant-message-created");
		return assistantMessage;
	}

	private void publishStreamEventSafely(Runnable task, String eventName) {
		try {
			task.run();
		} catch (RuntimeException exception) {
			log.warn("AI conversation stream publish failed eventName={}", eventName);
			log.debug("AI conversation stream publish failure detail", exception);
		}
	}

	private void recordFailureUsage(
		SendAiConversationMessageCommand command,
		AiConversationMessageContext context,
		AiConversationResponseGenerationException exception
	) {
		usageRecorder.record(exception.failure().toUsage(
			idGenerator.get(),
			command.authenticatedUserId(),
			context.groupId(),
			clock.instant()
		));
	}

	private void updateConversationSummaryIfNeeded(
		AiConversationMessageContext context,
		String conversationSummaryPatch,
		Instant updatedAt
	) {
		if (conversationSummaryPatch == null || conversationSummaryPatch.isBlank()) {
			return;
		}
		String summary = mergeSummary(context.summary(), conversationSummaryPatch);
		if (!repository.updateConversationSummary(context.conversationId(), summary, updatedAt)) {
			throw new AiConversationMutationRejectedException("AI conversation summary could not be updated.");
		}
	}

	private boolean assistantGenerationConfigured() {
		return assistantResponseGenerator != null && usageRecorder != null;
	}

	private static Map<String, Object> assistantMetadata(Map<String, Object> responseMetadata) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("retrievalContextVersion", "db-first-v1");
		if (responseMetadata != null) {
			metadata.putAll(responseMetadata);
		}
		return Map.copyOf(metadata);
	}

	private static String mergeSummary(String currentSummary, String patch) {
		String normalizedPatch = patch.strip();
		if (currentSummary == null || currentSummary.isBlank()) {
			return normalizedPatch;
		}
		return currentSummary.strip() + "\n" + normalizedPatch;
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

	private static String encodeCursor(AiConversationMessage message) {
		String rawCursor = message.createdAt() + "|" + message.id();
		return CURSOR_ENCODER.encodeToString(rawCursor.getBytes(StandardCharsets.UTF_8));
	}

	private static AiConversationMessageCursor decodeCursor(String cursor) {
		if (cursor == null) {
			return null;
		}
		try {
			String decoded = new String(CURSOR_DECODER.decode(cursor), StandardCharsets.UTF_8);
			String[] parts = decoded.split("\\|", 2);
			if (parts.length != 2) {
				throw new IllegalArgumentException("cursor format is invalid.");
			}
			return new AiConversationMessageCursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
		} catch (IllegalArgumentException | DateTimeParseException exception) {
			throw new InvalidAiConversationRequestException("cursor", "cursor is invalid.");
		}
	}
}
