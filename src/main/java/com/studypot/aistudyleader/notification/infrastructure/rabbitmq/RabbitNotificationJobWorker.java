package com.studypot.aistudyleader.notification.infrastructure.rabbitmq;

import com.studypot.aistudyleader.notification.service.CreateNotificationCommand;
import com.studypot.aistudyleader.notification.service.NotificationService;
import com.studypot.aistudyleader.notification.service.RecordNotificationFailureCommand;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

class RabbitNotificationJobWorker {

	private static final Logger log = LoggerFactory.getLogger(RabbitNotificationJobWorker.class);

	private final NotificationService notifications;

	RabbitNotificationJobWorker(NotificationService notifications) {
		this.notifications = Objects.requireNonNull(notifications, "notifications must not be null");
	}

	@RabbitListener(queues = "${studypot.notification.rabbitmq.queue:studypot.notification.jobs}")
	void handle(CreateNotificationCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		try {
			notifications.createNotification(command);
		} catch (RuntimeException exception) {
			recordFailure(command, exception);
			throw new AmqpRejectAndDontRequeueException(
				"notification job failed idempotencyKey=" + command.idempotencyKey(),
				exception
			);
		}
	}

	private void recordFailure(CreateNotificationCommand command, RuntimeException exception) {
		try {
			notifications.recordNotificationFailure(new RecordNotificationFailureCommand(command, exception.getMessage()));
		} catch (RuntimeException failureException) {
			exception.addSuppressed(failureException);
			log.warn("notification job failure record failed idempotencyKey={}", command.idempotencyKey());
			log.debug("notification job failure record detail", failureException);
		}
	}
}
