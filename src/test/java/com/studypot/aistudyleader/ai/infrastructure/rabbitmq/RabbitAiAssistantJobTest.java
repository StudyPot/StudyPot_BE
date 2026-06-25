package com.studypot.aistudyleader.ai.infrastructure.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studypot.aistudyleader.ai.service.AiAssistantJobDispatcher;
import com.studypot.aistudyleader.ai.service.AiConversationService;
import com.studypot.aistudyleader.ai.service.GenerateAiAssistantMessageJob;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.beans.factory.ObjectProvider;

class RabbitAiAssistantJobTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000009001");
	private static final UUID CONVERSATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000009002");
	private static final UUID USER_MESSAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000009003");

	private static final AiAssistantRabbitProperties PROPERTIES = new AiAssistantRabbitProperties(
		true,
		"studypot.ai.conversation.events",
		"studypot.ai.conversation.jobs",
		"ai.conversation.generate"
	);

	@Test
	void publisherSendsGenerationJobToConfiguredExchangeAndRoutingKey() {
		RabbitOperations rabbit = mock(RabbitOperations.class);
		AiAssistantJobDispatcher publisher = new RabbitAiAssistantJobPublisher(rabbit, PROPERTIES);

		// 활성 트랜잭션이 없으면 즉시 발행하고, assistant 는 worker 가 만들므로 결과는 비어 있다.
		Optional<?> result = publisher.dispatch(USER_ID, CONVERSATION_ID, USER_MESSAGE_ID);

		assertThat(result).isEmpty();
		ArgumentCaptor<GenerateAiAssistantMessageJob> jobCaptor = ArgumentCaptor.forClass(GenerateAiAssistantMessageJob.class);
		verify(rabbit).convertAndSend(eq("studypot.ai.conversation.events"), eq("ai.conversation.generate"), jobCaptor.capture());
		GenerateAiAssistantMessageJob job = jobCaptor.getValue();
		assertThat(job.authenticatedUserId()).isEqualTo(USER_ID);
		assertThat(job.conversationId()).isEqualTo(CONVERSATION_ID);
		assertThat(job.userMessageId()).isEqualTo(USER_MESSAGE_ID);
	}

	@Test
	@SuppressWarnings("unchecked")
	void workerDelegatesGenerationToConversationService() {
		AiConversationService service = mock(AiConversationService.class);
		ObjectProvider<AiConversationService> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(service);
		RabbitAiAssistantJobWorker worker = new RabbitAiAssistantJobWorker(provider);

		worker.handle(new GenerateAiAssistantMessageJob(USER_ID, CONVERSATION_ID, USER_MESSAGE_ID));

		verify(service).generateAssistantReply(USER_ID, CONVERSATION_ID, USER_MESSAGE_ID);
	}

	@Test
	@SuppressWarnings("unchecked")
	void workerRejectsWithoutRequeueWhenGenerationFails() {
		AiConversationService service = mock(AiConversationService.class);
		ObjectProvider<AiConversationService> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(service);
		when(service.generateAssistantReply(USER_ID, CONVERSATION_ID, USER_MESSAGE_ID))
			.thenThrow(new IllegalStateException("openai timed out"));
		RabbitAiAssistantJobWorker worker = new RabbitAiAssistantJobWorker(provider);

		assertThatThrownBy(() -> worker.handle(new GenerateAiAssistantMessageJob(USER_ID, CONVERSATION_ID, USER_MESSAGE_ID)))
			.isInstanceOf(AmqpRejectAndDontRequeueException.class)
			.hasMessageContaining("AI assistant generation job failed");
	}

	@Test
	@SuppressWarnings("unchecked")
	void workerRejectsWhenConversationServiceIsUnavailable() {
		ObjectProvider<AiConversationService> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(null);
		RabbitAiAssistantJobWorker worker = new RabbitAiAssistantJobWorker(provider);

		assertThatThrownBy(() -> worker.handle(new GenerateAiAssistantMessageJob(USER_ID, CONVERSATION_ID, USER_MESSAGE_ID)))
			.isInstanceOf(AmqpRejectAndDontRequeueException.class);
	}

	@Test
	void propertiesRejectBlankRoutingValues() {
		assertThatThrownBy(() -> new AiAssistantRabbitProperties(true, " ", "queue", "ai.conversation.generate"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("exchange must not be blank.");
		assertThatThrownBy(() -> new AiAssistantRabbitProperties(true, "exchange", "", "ai.conversation.generate"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("queue must not be blank.");
		assertThatThrownBy(() -> new AiAssistantRabbitProperties(true, "exchange", "queue", "\t"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("routingKey must not be blank.");
	}
}
