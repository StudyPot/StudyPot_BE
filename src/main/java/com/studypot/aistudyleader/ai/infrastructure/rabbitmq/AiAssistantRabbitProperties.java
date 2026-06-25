package com.studypot.aistudyleader.ai.infrastructure.rabbitmq;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "studypot.ai.conversation.rabbitmq")
record AiAssistantRabbitProperties(
	boolean enabled,
	String exchange,
	String queue,
	String routingKey
) {

	AiAssistantRabbitProperties {
		exchange = requireText(exchange, "exchange");
		queue = requireText(queue, "queue");
		routingKey = requireText(routingKey, "routingKey");
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank.");
		}
		return value.strip();
	}
}
