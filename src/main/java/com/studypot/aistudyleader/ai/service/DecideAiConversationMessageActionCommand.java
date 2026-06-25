package com.studypot.aistudyleader.ai.service;

import java.util.Objects;
import java.util.UUID;

/**
 * AI 팀장 메시지에 제안된 액션을 확인/거절하는 명령.
 * instruction 이 있으면(= '기타' 직접 요청) 확정 시 그 지시대로 글을 재작성해 올린다.
 */
public record DecideAiConversationMessageActionCommand(
	UUID authenticatedUserId,
	UUID conversationId,
	UUID messageId,
	AiConversationMessageActionDecision decision,
	String instruction
) {

	public DecideAiConversationMessageActionCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(conversationId, "conversationId must not be null");
		Objects.requireNonNull(messageId, "messageId must not be null");
		Objects.requireNonNull(decision, "decision must not be null");
		instruction = instruction == null || instruction.isBlank() ? null : instruction.strip();
	}

	public DecideAiConversationMessageActionCommand(
		UUID authenticatedUserId,
		UUID conversationId,
		UUID messageId,
		AiConversationMessageActionDecision decision
	) {
		this(authenticatedUserId, conversationId, messageId, decision, null);
	}
}
