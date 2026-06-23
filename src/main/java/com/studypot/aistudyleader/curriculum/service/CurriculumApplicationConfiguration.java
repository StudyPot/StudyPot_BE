package com.studypot.aistudyleader.curriculum.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.curriculum.repository.CurriculumRepository;
import com.studypot.aistudyleader.global.domain.UuidV7;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmProviderConfiguredCondition;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import com.studypot.aistudyleader.notification.service.NotificationEventPublisher;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
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
		return new CurriculumService(
			repository,
			generator::getIfAvailable,
			clock,
			UuidV7::generate,
			notificationEvents.getIfAvailable(NotificationEventPublisher::noop)
		);
	}

	@Bean
	@Conditional(LlmProviderConfiguredCondition.class)
	NextWeekPlanGenerator nextWeekPlanGenerator(LlmProviderClient provider, ObjectMapper objectMapper) {
		return new ProviderBackedNextWeekPlanGenerator(provider, objectMapper);
	}

	@Bean
	@ConditionalOnBean(CurriculumRepository.class)
	NextWeekPlanService nextWeekPlanService(
		CurriculumRepository repository,
		ObjectProvider<NextWeekPlanGenerator> generator,
		ObjectProvider<LlmUsageRecorder> usageRecorder,
		Clock clock
	) {
		return new NextWeekPlanService(
			repository,
			generator::getIfAvailable,
			usageRecorder::getIfAvailable,
			clock,
			UuidV7::generate
		);
	}
}
