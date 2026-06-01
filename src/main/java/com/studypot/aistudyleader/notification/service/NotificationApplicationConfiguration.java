package com.studypot.aistudyleader.notification.service;

import com.studypot.aistudyleader.global.domain.UuidV7;
import com.studypot.aistudyleader.notification.repository.NotificationRepository;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class NotificationApplicationConfiguration {

	@Bean
	@ConditionalOnBean({NotificationRepository.class, Clock.class})
	NotificationService notificationService(
		NotificationRepository repository,
		Clock clock,
		ObjectProvider<NotificationStreamPublisher> streamPublisher
	) {
		return new NotificationService(repository, clock, UuidV7::generate,
			streamPublisher.getIfAvailable(NotificationStreamPublisher::noop));
	}
}
