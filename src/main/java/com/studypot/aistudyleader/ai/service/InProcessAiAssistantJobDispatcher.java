package com.studypot.aistudyleader.ai.service;

import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 기본 디스패처. RabbitMQ 가 비활성일 때 사용되며, 현재 트랜잭션 안에서 동기로 AI 응답을 생성한다.
 * (메시지 큐 도입 전 동작과 동일하게 유지하기 위한 폴백 경로.)
 *
 * <p>{@link AiConversationService} 와의 순환 의존을 피하기 위해 {@link ObjectProvider} 로 지연 해소한다.
 */
public class InProcessAiAssistantJobDispatcher implements AiAssistantJobDispatcher {

	private final ObjectProvider<AiConversationService> conversationService;

	public InProcessAiAssistantJobDispatcher(ObjectProvider<AiConversationService> conversationService) {
		this.conversationService = Objects.requireNonNull(conversationService, "conversationService must not be null");
	}

	@Override
	public Optional<AiConversationMessage> dispatch(UUID authenticatedUserId, UUID conversationId, UUID userMessageId) {
		AiConversationService service = conversationService.getIfAvailable();
		if (service == null) {
			throw new AiConversationServiceUnavailableException("AI conversation service is not configured.");
		}
		return Optional.of(service.generateAssistantReply(authenticatedUserId, conversationId, userMessageId));
	}
}
