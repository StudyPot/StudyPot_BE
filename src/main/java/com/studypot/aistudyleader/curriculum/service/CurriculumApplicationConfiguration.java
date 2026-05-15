package com.studypot.aistudyleader.curriculum.service;

import com.studypot.aistudyleader.curriculum.repository.CurriculumRepository;
import com.studypot.aistudyleader.global.domain.UuidV7;
import com.studypot.aistudyleader.notification.service.NotificationEventPublisher;
import java.time.Clock;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class CurriculumApplicationConfiguration {

	@Bean
	@ConditionalOnBean(CurriculumRepository.class)
	CurriculumService curriculumService(
		CurriculumRepository repository,
		ObjectProvider<CurriculumGenerator> generator,
		Clock clock,
		ObjectProvider<NotificationEventPublisher> notificationEvents
	) {
		CurriculumGenerator configuredGenerator = generator.getIfAvailable();
		Supplier<CurriculumGenerator> generatorSupplier = () -> configuredGenerator;
		return new CurriculumService(
			repository,
			generatorSupplier,
			clock,
			UuidV7::generate,
			notificationEvents.getIfAvailable(NotificationEventPublisher::noop)
		);
	}
}
