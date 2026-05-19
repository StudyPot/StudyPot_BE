package com.studypot.aistudyleader.notification.infrastructure.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.studypot.aistudyleader.notification.repository.NotificationRepository;
import com.studypot.aistudyleader.notification.service.NotificationEventPublisher;
import com.studypot.aistudyleader.notification.service.NotificationJobPublisher;
import com.studypot.aistudyleader.notification.service.NotificationService;
import com.studypot.aistudyleader.notification.service.QueuedNotificationEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RabbitNotificationConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
		.withUserConfiguration(RabbitNotificationConfiguration.class)
		.withBean(RabbitOperations.class, () -> mock(RabbitOperations.class))
		.withBean(NotificationRepository.class, () -> mock(NotificationRepository.class))
		.withBean(NotificationService.class, () -> mock(NotificationService.class))
		.withPropertyValues(
			"studypot.notification.rabbitmq.exchange=studypot.notification.events",
			"studypot.notification.rabbitmq.queue=studypot.notification.events",
			"studypot.notification.rabbitmq.routing-key=notification.create"
		);

	@Test
	void rabbitNotificationWorkerIsDisabledByDefault() {
		contextRunner.run(context -> {
			assertThat(context).doesNotHaveBean(NotificationRabbitProperties.class);
			assertThat(context).doesNotHaveBean(NotificationJobPublisher.class);
			assertThat(context).doesNotHaveBean(RabbitNotificationJobWorker.class);
		});
	}

	@Test
	void enabledRabbitNotificationWorkerRegistersQueueAndPrimaryEventPublisher() {
		contextRunner
			.withPropertyValues("studypot.notification.rabbitmq.enabled=true")
			.run(context -> {
				assertThat(context).hasSingleBean(NotificationRabbitProperties.class);
				assertThat(context).hasSingleBean(NotificationJobPublisher.class);
				assertThat(context).hasSingleBean(RabbitNotificationJobWorker.class);
				assertThat(context).hasSingleBean(DirectExchange.class);
				assertThat(context).hasSingleBean(Queue.class);
				assertThat(context).hasSingleBean(Binding.class);
				assertThat(context.getBean(NotificationEventPublisher.class))
					.isInstanceOf(QueuedNotificationEventPublisher.class);
			});
	}
}
