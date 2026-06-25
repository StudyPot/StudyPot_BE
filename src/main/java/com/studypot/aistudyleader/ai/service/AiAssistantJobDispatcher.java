package com.studypot.aistudyleader.ai.service;

import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import java.util.Optional;
import java.util.UUID;

/**
 * 사용자 메시지 저장 후 AI 팀장 응답 생성을 어떻게 처리할지 결정하는 디스패처.
 * <ul>
 *   <li>in-process(기본): 현재 트랜잭션에서 동기로 생성하고 생성된 assistant 메시지를 반환한다.</li>
 *   <li>RabbitMQ: 작업을 큐에 발행(커밋 후)하고 비어 있는 결과를 반환한다. assistant 메시지는 worker 가 만들어 SSE 로 전달한다.</li>
 * </ul>
 */
public interface AiAssistantJobDispatcher {

	/**
	 * @return 동기 처리 시 생성된 assistant 메시지, 비동기(큐) 처리 시 {@link Optional#empty()}.
	 */
	Optional<AiConversationMessage> dispatch(UUID authenticatedUserId, UUID conversationId, UUID userMessageId);
}
