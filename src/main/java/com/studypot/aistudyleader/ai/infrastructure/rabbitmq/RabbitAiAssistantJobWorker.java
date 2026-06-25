package com.studypot.aistudyleader.ai.infrastructure.rabbitmq;

import com.studypot.aistudyleader.ai.service.AiConversationService;
import com.studypot.aistudyleader.ai.service.GenerateAiAssistantMessageJob;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 큐에 실린 AI 응답 생성 작업을 같은 JVM 안에서 소비한다(SSE 연결은 인메모리라 동일 인스턴스에서 푸시해야 함).
 * 생성 실패 시 {@link AiConversationService#generateAssistantReply} 가 이미 실패 SSE 를 발행하므로,
 * 여기서는 재시도 없이 reject 하여 같은 작업이 무한 재생성되는 것을 막는다.
 */
class RabbitAiAssistantJobWorker {

	private static final Logger log = LoggerFactory.getLogger(RabbitAiAssistantJobWorker.class);

	private final ObjectProvider<AiConversationService> conversationService;

	RabbitAiAssistantJobWorker(ObjectProvider<AiConversationService> conversationService) {
		this.conversationService = Objects.requireNonNull(conversationService, "conversationService must not be null");
	}

	@RabbitListener(queues = "${studypot.ai.conversation.rabbitmq.queue:studypot.ai.conversation.jobs}")
	void handle(GenerateAiAssistantMessageJob job) {
		Objects.requireNonNull(job, "job must not be null");
		AiConversationService service = conversationService.getIfAvailable();
		if (service == null) {
			throw new AmqpRejectAndDontRequeueException("AI conversation service is not configured.");
		}
		try {
			service.generateAssistantReply(job.authenticatedUserId(), job.conversationId(), job.userMessageId());
		} catch (RuntimeException exception) {
			log.warn("AI assistant generation job failed conversationId={} userMessageId={}",
				job.conversationId(), job.userMessageId());
			log.debug("AI assistant generation job failure detail", exception);
			throw new AmqpRejectAndDontRequeueException(
				"AI assistant generation job failed userMessageId=" + job.userMessageId(),
				exception
			);
		}
	}
}
