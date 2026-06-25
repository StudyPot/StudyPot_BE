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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class AiConversationService {

	private static final Base64.Encoder CURSOR_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder CURSOR_DECODER = Base64.getUrlDecoder();
	private static final Logger log = LoggerFactory.getLogger(AiConversationService.class);

	private static final String RETROSPECTIVE_GREETING = """
		안녕하세요, AI 팀장이에요. 이번 주 회고를 함께 정리해볼게요. 편하게 답해 주세요.
		1) 이번 주는 전반적으로 어떠셨나요?
		2) TODO에서 진행한 것들 중 가장 어려웠던 건 무엇이었나요?
		3) 다음 주에는 무엇을, 어떻게 해보고 싶으세요?
		답변을 주시면 그걸 바탕으로 다음 주 학습 내용을 함께 조정해드릴게요.""";

	private final AiConversationRepository repository;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;
	private final AiConversationAssistantResponseGenerator assistantResponseGenerator;
	private final LlmUsageRecorder usageRecorder;
	private final AiConversationStreamPublisher streamPublisher;
	private final AiConversationBoardGateway boardGateway;
	private final AiConversationQuestionRefiner questionRefiner;
	private final AiConversationCurriculumGateway curriculumGateway;

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
		this(repository, clock, idGenerator, assistantResponseGenerator, usageRecorder, streamPublisher, null);
	}

	public AiConversationService(
		AiConversationRepository repository,
		Clock clock,
		Supplier<UUID> idGenerator,
		AiConversationAssistantResponseGenerator assistantResponseGenerator,
		LlmUsageRecorder usageRecorder,
		AiConversationStreamPublisher streamPublisher,
		AiConversationBoardGateway boardGateway
	) {
		this(repository, clock, idGenerator, assistantResponseGenerator, usageRecorder, streamPublisher, boardGateway, null);
	}

	public AiConversationService(
		AiConversationRepository repository,
		Clock clock,
		Supplier<UUID> idGenerator,
		AiConversationAssistantResponseGenerator assistantResponseGenerator,
		LlmUsageRecorder usageRecorder,
		AiConversationStreamPublisher streamPublisher,
		AiConversationBoardGateway boardGateway,
		AiConversationQuestionRefiner questionRefiner
	) {
		this(repository, clock, idGenerator, assistantResponseGenerator, usageRecorder, streamPublisher, boardGateway, questionRefiner, null);
	}

	public AiConversationService(
		AiConversationRepository repository,
		Clock clock,
		Supplier<UUID> idGenerator,
		AiConversationAssistantResponseGenerator assistantResponseGenerator,
		LlmUsageRecorder usageRecorder,
		AiConversationStreamPublisher streamPublisher,
		AiConversationBoardGateway boardGateway,
		AiConversationQuestionRefiner questionRefiner,
		AiConversationCurriculumGateway curriculumGateway
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
		this.assistantResponseGenerator = assistantResponseGenerator;
		this.usageRecorder = usageRecorder;
		this.streamPublisher = Objects.requireNonNull(streamPublisher, "streamPublisher must not be null");
		this.boardGateway = boardGateway;
		this.questionRefiner = questionRefiner;
		this.curriculumGateway = curriculumGateway;
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
		if (isOrdinaryTeamLeadChat(command, effectiveWeekId)) {
			Optional<AiConversation> existingConversation = repository.findOpenTeamLeadConversation(command.groupId(), context.memberId());
			if (existingConversation.isPresent()) {
				return existingConversation.get();
			}
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
		if (conversation.conversationType() == AiConversationType.RETROSPECTIVE) {
			seedRetrospectiveGreeting(conversation, now);
		}
		return conversation;
	}

	private void seedRetrospectiveGreeting(AiConversation conversation, Instant now) {
		AiConversationMessage greeting = AiConversationMessage.assistantSeedMessage(
			idGenerator.get(),
			conversation.id(),
			RETROSPECTIVE_GREETING,
			Map.of("seed", true, "kind", "retrospective_intro"),
			now
		);
		if (repository.insertMessage(greeting)) {
			publishStreamEventSafely(() -> streamPublisher.publishAssistantMessageCreated(greeting), "retrospective-seed");
		}
	}

	private static boolean isOrdinaryTeamLeadChat(OpenAiConversationCommand command, UUID effectiveWeekId) {
		return command.conversationType() == AiConversationType.TEAM_LEAD_CHAT
			&& effectiveWeekId == null
			&& command.retrospectiveId() == null;
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

	/**
	 * AI 팀장 메시지에 제안된 액션(현재 SHARE_QUESTION)을 사용자가 확인/거절한다(확인 후 실행 모델).
	 * CONFIRM 이면 권한 검사 후 실제 실행(질문 게시판 등록)하고 결과를 메시지 metadata 에 기록한다.
	 * 반환값은 액션 상태가 갱신된 원본 메시지(프론트 버튼 상태 갱신용).
	 */
	@Transactional
	public AiConversationMessage decideMessageAction(DecideAiConversationMessageActionCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		AiConversationMessageContext context = requireMessageContext(command.conversationId(), command.authenticatedUserId());
		if (!context.hasActiveMembership()) {
			throw new AiConversationAccessDeniedException("active group membership is required to act on an AI conversation message.");
		}
		AiConversationMessage message = repository.findMessage(command.messageId())
			.orElseThrow(() -> new AiConversationNotFoundException("AI conversation message was not found."));
		if (!message.conversationId().equals(command.conversationId())) {
			throw new AiConversationNotFoundException("AI conversation message was not found.");
		}
		Map<String, Object> metadata = new LinkedHashMap<>(message.metadata());
		Map<String, Object> pendingAction = asStringMap(metadata.get("pendingAction"));
		if (pendingAction.isEmpty()) {
			throw new AiConversationMutationRejectedException("this message has no pending action to decide.");
		}
		if (!"PENDING".equals(pendingAction.get("status"))) {
			throw new AiConversationMutationRejectedException("this action has already been decided.");
		}
		Instant now = clock.instant();
		if (command.decision() == AiConversationMessageActionDecision.REJECT) {
			pendingAction.put("status", "REJECTED");
			metadata.put("pendingAction", pendingAction);
			persistMessageMetadata(command.messageId(), metadata);
			return message.withMetadata(metadata);
		}
		String type = String.valueOf(pendingAction.get("type"));
		switch (type) {
			case "SHARE_QUESTION" -> executeShareQuestion(context, command, pendingAction, now);
			case "COMPLETE_TASK" -> executeCompleteTask(command, pendingAction, now);
			case "ADD_TASK" -> executeAddTask(context, command, pendingAction, now);
			case "EDIT_POST" -> executeEditPost(context, command, pendingAction, now);
			case "DELETE_POST" -> executeDeletePost(context, command, pendingAction, now);
			default -> throw new AiConversationMutationRejectedException("unsupported AI conversation action type: " + type + ".");
		}
		metadata.put("pendingAction", pendingAction);
		persistMessageMetadata(command.messageId(), metadata);
		return message.withMetadata(metadata);
	}

	private void executeCompleteTask(
		DecideAiConversationMessageActionCommand command,
		Map<String, Object> pendingAction,
		Instant now
	) {
		if (curriculumGateway == null) {
			throw new AiConversationServiceUnavailableException("AI conversation curriculum action is not configured.");
		}
		String taskId = stringValue(pendingAction.get("taskId"));
		String completionStatus = stringValue(pendingAction.get("completionStatus"));
		if (taskId.isBlank()) {
			throw new AiConversationMutationRejectedException("the proposed task action is incomplete.");
		}
		if (completionStatus.isBlank()) {
			completionStatus = "DONE";
		}
		curriculumGateway.completeTask(command.authenticatedUserId(), UUID.fromString(taskId), completionStatus);
		pendingAction.put("status", "EXECUTED");
		String label = "DONE".equals(completionStatus) ? "완료" : "미완료";
		AiConversationMessage confirmation = AiConversationMessage.assistantSeedMessage(
			idGenerator.get(),
			command.conversationId(),
			"과제를 " + label + " 처리했어. ✅",
			Map.of("kind", "action_result", "action", "COMPLETE_TASK"),
			now
		);
		if (repository.insertMessage(confirmation)) {
			publishStreamEventSafely(() -> streamPublisher.publishAssistantMessageCreated(confirmation), "assistant-message-created");
		}
	}

	private void executeShareQuestion(
		AiConversationMessageContext context,
		DecideAiConversationMessageActionCommand command,
		Map<String, Object> pendingAction,
		Instant now
	) {
		if (boardGateway == null) {
			throw new AiConversationServiceUnavailableException("AI conversation board action is not configured.");
		}
		Map<String, Object> question = asStringMap(pendingAction.get("question"));
		String title = stringValue(question.get("title"));
		String summary = stringValue(question.get("summary"));
		if (title.isBlank() || summary.isBlank()) {
			throw new AiConversationMutationRejectedException("the proposed question to share is incomplete.");
		}
		// '기타' 직접 요청: 사용자가 지시를 줬고 재작성기가 있으면 지시대로 제목/본문을 다시 작성한다.
		// 재작성 실패 시 원래 초안으로 등록(통째 실패보다 등록을 우선).
		String postTitle = title;
		String postContent = summary;
		if (command.instruction() != null && questionRefiner != null) {
			try {
				RefinedQuestionPost refined = questionRefiner.refine(
					command.authenticatedUserId(), context.groupId(), title, summary, command.instruction());
				postTitle = refined.title();
				postContent = refined.content();
			} catch (RuntimeException exception) {
				log.warn("question share refine failed; falling back to original draft");
				log.debug("question share refine failure detail", exception);
			}
		}
		UUID postId = boardGateway.shareQuestionToBoard(command.authenticatedUserId(), context.groupId(), postTitle, postContent);
		pendingAction.put("status", "EXECUTED");
		pendingAction.put("result", Map.of("postId", postId.toString(), "boardType", "QUESTION"));
		AiConversationMessage confirmation = AiConversationMessage.assistantSeedMessage(
			idGenerator.get(),
			command.conversationId(),
			"질문을 '질문' 게시판에 올렸어. 다른 멤버들도 볼 수 있어. ✅",
			Map.of("kind", "action_result", "action", "SHARE_QUESTION", "postId", postId.toString()),
			now
		);
		if (repository.insertMessage(confirmation)) {
			publishStreamEventSafely(() -> streamPublisher.publishAssistantMessageCreated(confirmation), "assistant-message-created");
		}
	}

	private void executeEditPost(
		AiConversationMessageContext context,
		DecideAiConversationMessageActionCommand command,
		Map<String, Object> pendingAction,
		Instant now
	) {
		if (boardGateway == null) {
			throw new AiConversationServiceUnavailableException("AI conversation board action is not configured.");
		}
		String postId = stringValue(pendingAction.get("postId"));
		String title = stringValue(pendingAction.get("title"));
		String summary = stringValue(pendingAction.get("summary"));
		if (postId.isBlank() || (title.isBlank() && summary.isBlank())) {
			throw new AiConversationMutationRejectedException("the proposed post edit is incomplete.");
		}
		boardGateway.updatePostOnBoard(
			command.authenticatedUserId(),
			context.groupId(),
			UUID.fromString(postId),
			title.isBlank() ? null : title,
			summary.isBlank() ? null : summary
		);
		pendingAction.put("status", "EXECUTED");
		pendingAction.put("result", Map.of("postId", postId, "boardType", "QUESTION"));
		AiConversationMessage confirmation = AiConversationMessage.assistantSeedMessage(
			idGenerator.get(),
			command.conversationId(),
			"게시글을 수정했어. ✅",
			Map.of("kind", "action_result", "action", "EDIT_POST", "postId", postId),
			now
		);
		if (repository.insertMessage(confirmation)) {
			publishStreamEventSafely(() -> streamPublisher.publishAssistantMessageCreated(confirmation), "assistant-message-created");
		}
	}

	private void executeDeletePost(
		AiConversationMessageContext context,
		DecideAiConversationMessageActionCommand command,
		Map<String, Object> pendingAction,
		Instant now
	) {
		if (boardGateway == null) {
			throw new AiConversationServiceUnavailableException("AI conversation board action is not configured.");
		}
		String postId = stringValue(pendingAction.get("postId"));
		if (postId.isBlank()) {
			throw new AiConversationMutationRejectedException("the proposed post deletion is incomplete.");
		}
		boardGateway.deletePostOnBoard(command.authenticatedUserId(), context.groupId(), UUID.fromString(postId));
		pendingAction.put("status", "EXECUTED");
		AiConversationMessage confirmation = AiConversationMessage.assistantSeedMessage(
			idGenerator.get(),
			command.conversationId(),
			"게시글을 삭제했어. ✅",
			Map.of("kind", "action_result", "action", "DELETE_POST"),
			now
		);
		if (repository.insertMessage(confirmation)) {
			publishStreamEventSafely(() -> streamPublisher.publishAssistantMessageCreated(confirmation), "assistant-message-created");
		}
	}

	private void executeAddTask(
		AiConversationMessageContext context,
		DecideAiConversationMessageActionCommand command,
		Map<String, Object> pendingAction,
		Instant now
	) {
		if (curriculumGateway == null) {
			throw new AiConversationServiceUnavailableException("AI conversation curriculum action is not configured.");
		}
		String title = stringValue(pendingAction.get("title"));
		String description = stringValue(pendingAction.get("description"));
		if (title.isBlank()) {
			throw new AiConversationMutationRejectedException("the proposed task to add is incomplete.");
		}
		curriculumGateway.addTaskToCurrentWeek(
			command.authenticatedUserId(), context.groupId(), title, description.isBlank() ? null : description);
		pendingAction.put("status", "EXECUTED");
		AiConversationMessage confirmation = AiConversationMessage.assistantSeedMessage(
			idGenerator.get(),
			command.conversationId(),
			"이번 주 과제에 추가했어. ✅",
			Map.of("kind", "action_result", "action", "ADD_TASK"),
			now
		);
		if (repository.insertMessage(confirmation)) {
			publishStreamEventSafely(() -> streamPublisher.publishAssistantMessageCreated(confirmation), "assistant-message-created");
		}
	}

	private void persistMessageMetadata(UUID messageId, Map<String, Object> metadata) {
		if (!repository.updateMessageMetadata(messageId, metadata)) {
			throw new AiConversationMutationRejectedException("AI conversation message metadata could not be updated.");
		}
	}

	private static Map<String, Object> asStringMap(Object value) {
		Map<String, Object> result = new LinkedHashMap<>();
		if (value instanceof Map<?, ?> map) {
			map.forEach((key, entryValue) -> result.put(String.valueOf(key), entryValue));
		}
		return result;
	}

	private static String stringValue(Object value) {
		return value == null ? "" : value.toString().strip();
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
