package com.studypot.aistudyleader.ai.infrastructure.rabbitmq;

import com.studypot.aistudyleader.ai.service.AiAssistantJobDispatcher;
import com.studypot.aistudyleader.ai.service.AiConversationService;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AI 팀장 응답 생성을 RabbitMQ 로 비동기 처리하기 위한 구성.
 * {@code studypot.ai.conversation.rabbitmq.enabled=true} 일 때만 활성화되며, 비활성 시에는
 * {@link com.studypot.aistudyleader.ai.service.InProcessAiAssistantJobDispatcher} 동기 폴백이 사용된다.
 * worker 는 인메모리 SSE 연결에 푸시해야 하므로 알림 worker 와 동일하게 같은 JVM 안에서 동작한다.
 */
@EnableRabbit
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiAssistantRabbitProperties.class)
@ConditionalOnProperty(prefix = "studypot.ai.conversation.rabbitmq", name = "enabled", havingValue = "true")
class RabbitAiAssistantConfiguration {

	@Bean
	DirectExchange aiAssistantRabbitExchange(AiAssistantRabbitProperties properties) {
		return new DirectExchange(properties.exchange(), true, false);
	}

	@Bean
	Queue aiAssistantRabbitQueue(AiAssistantRabbitProperties properties) {
		return QueueBuilder.durable(properties.queue()).build();
	}

	@Bean
	Binding aiAssistantRabbitBinding(Queue aiAssistantRabbitQueue, DirectExchange aiAssistantRabbitExchange, AiAssistantRabbitProperties properties) {
		return BindingBuilder.bind(aiAssistantRabbitQueue)
			.to(aiAssistantRabbitExchange)
			.with(properties.routingKey());
	}

	@Bean
	MessageConverter aiAssistantRabbitMessageConverter() {
		return new JacksonJsonMessageConverter();
	}

	@Bean
	@Primary
	AiAssistantJobDispatcher rabbitAiAssistantJobPublisher(RabbitOperations rabbit, AiAssistantRabbitProperties properties) {
		return new RabbitAiAssistantJobPublisher(rabbit, properties);
	}

	@Bean
	RabbitAiAssistantJobWorker rabbitAiAssistantJobWorker(ObjectProvider<AiConversationService> conversationService) {
		return new RabbitAiAssistantJobWorker(conversationService);
	}
}
