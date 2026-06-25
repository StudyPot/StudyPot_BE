package com.studypot.aistudyleader.ai.infrastructure.rabbitmq;

import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import com.studypot.aistudyleader.ai.service.AiAssistantJobDispatcher;
import com.studypot.aistudyleader.ai.service.GenerateAiAssistantMessageJob;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * RabbitMQ 기반 디스패처. 사용자 메시지 저장 트랜잭션이 커밋된 뒤 작업을 큐에 발행한다.
 * worker 가 DB 에서 사용자 메시지를 다시 읽으므로 커밋 후 발행이 필수다(커밋 전 발행 시 worker 가 못 찾음).
 * 응답은 worker 가 생성해 SSE 로 전달하므로 여기서는 {@link Optional#empty()} 를 반환한다.
 */
public class RabbitAiAssistantJobPublisher implements AiAssistantJobDispatcher {

	private static final Logger log = LoggerFactory.getLogger(RabbitAiAssistantJobPublisher.class);

	private final RabbitOperations rabbit;
	private final AiAssistantRabbitProperties properties;

	RabbitAiAssistantJobPublisher(RabbitOperations rabbit, AiAssistantRabbitProperties properties) {
		this.rabbit = Objects.requireNonNull(rabbit, "rabbit must not be null");
		this.properties = Objects.requireNonNull(properties, "properties must not be null");
	}

	@Override
	public Optional<AiConversationMessage> dispatch(UUID authenticatedUserId, UUID conversationId, UUID userMessageId) {
		GenerateAiAssistantMessageJob job = new GenerateAiAssistantMessageJob(authenticatedUserId, conversationId, userMessageId);
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					publish(job);
				}
			});
		} else {
			publish(job);
		}
		return Optional.empty();
	}

	private void publish(GenerateAiAssistantMessageJob job) {
		rabbit.convertAndSend(properties.exchange(), properties.routingKey(), job);
	}
}
