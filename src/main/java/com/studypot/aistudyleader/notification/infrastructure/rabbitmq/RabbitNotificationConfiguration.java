package com.studypot.aistudyleader.notification.infrastructure.rabbitmq;

import com.studypot.aistudyleader.notification.repository.NotificationRepository;
import com.studypot.aistudyleader.notification.service.NotificationEventPublisher;
import com.studypot.aistudyleader.notification.service.NotificationJobPublisher;
import com.studypot.aistudyleader.notification.service.NotificationService;
import com.studypot.aistudyleader.notification.service.QueuedNotificationEventPublisher;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@EnableRabbit
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NotificationRabbitProperties.class)
@ConditionalOnProperty(prefix = "studypot.notification.rabbitmq", name = "enabled", havingValue = "true")
class RabbitNotificationConfiguration {

	@Bean
	DirectExchange notificationRabbitExchange(NotificationRabbitProperties properties) {
		return new DirectExchange(properties.exchange(), true, false);
	}

	@Bean
	Queue notificationRabbitQueue(NotificationRabbitProperties properties) {
		return QueueBuilder.durable(properties.queue()).build();
	}

	@Bean
	Binding notificationRabbitBinding(Queue notificationRabbitQueue, DirectExchange notificationRabbitExchange, NotificationRabbitProperties properties) {
		return BindingBuilder.bind(notificationRabbitQueue)
			.to(notificationRabbitExchange)
			.with(properties.routingKey());
	}

	@Bean
	MessageConverter notificationRabbitMessageConverter() {
		return new JacksonJsonMessageConverter();
	}

	@Bean
	NotificationJobPublisher rabbitNotificationJobPublisher(RabbitOperations rabbit, NotificationRabbitProperties properties) {
		return new RabbitNotificationJobPublisher(rabbit, properties);
	}

	@Bean
	@Primary
	@ConditionalOnBean({NotificationRepository.class, NotificationJobPublisher.class})
	NotificationEventPublisher queuedNotificationEventPublisher(NotificationRepository repository, NotificationJobPublisher jobs) {
		return new QueuedNotificationEventPublisher(repository, jobs);
	}

	@Bean
	RabbitNotificationJobWorker rabbitNotificationJobWorker(NotificationService notifications) {
		return new RabbitNotificationJobWorker(notifications);
	}
}
