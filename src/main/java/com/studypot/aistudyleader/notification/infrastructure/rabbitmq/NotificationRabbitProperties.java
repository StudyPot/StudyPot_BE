package com.studypot.aistudyleader.notification.infrastructure.rabbitmq;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "studypot.notification.rabbitmq")
record NotificationRabbitProperties(
	boolean enabled,
	String exchange,
	String queue,
	String routingKey
) {

	NotificationRabbitProperties {
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
