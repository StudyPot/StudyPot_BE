package com.studypot.aistudyleader.ai.service;

import java.util.Objects;
import java.util.UUID;

/**
 * AI 팀장 응답 생성을 비동기로 처리하기 위한 작업 메시지.
 * RabbitMQ 경유 시 직렬화되어 큐에 실리며, worker 가 받아 {@link AiConversationService#generateAssistantReply} 를 호출한다.
 * worker 는 DB 에서 사용자 메시지를 다시 읽으므로, 사용자 메시지 insert 가 커밋된 뒤에만 발행해야 한다.
 */
public record GenerateAiAssistantMessageJob(
	UUID authenticatedUserId,
	UUID conversationId,
	UUID userMessageId
) {

	public GenerateAiAssistantMessageJob {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(conversationId, "conversationId must not be null");
		Objects.requireNonNull(userMessageId, "userMessageId must not be null");
	}
}
