package com.studypot.aistudyleader.notification.infrastructure.rabbitmq;

import com.studypot.aistudyleader.notification.service.CreateNotificationCommand;
import com.studypot.aistudyleader.notification.service.NotificationJobPublisher;
import java.util.Objects;
import org.springframework.amqp.rabbit.core.RabbitOperations;

class RabbitNotificationJobPublisher implements NotificationJobPublisher {

	private final RabbitOperations rabbit;
	private final NotificationRabbitProperties properties;

	RabbitNotificationJobPublisher(RabbitOperations rabbit, NotificationRabbitProperties properties) {
		this.rabbit = Objects.requireNonNull(rabbit, "rabbit must not be null");
		this.properties = Objects.requireNonNull(properties, "properties must not be null");
	}

	@Override
	public void publish(CreateNotificationCommand command) {
		rabbit.convertAndSend(
			properties.exchange(),
			properties.routingKey(),
			Objects.requireNonNull(command, "command must not be null")
		);
	}
}
