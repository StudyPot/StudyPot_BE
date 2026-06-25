package com.studypot.aistudyleader.retrospective.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.global.domain.UuidV7;
import com.studypot.aistudyleader.llm.service.LlmProviderConfiguredCondition;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import com.studypot.aistudyleader.notification.service.NotificationEventPublisher;
import com.studypot.aistudyleader.retrospective.repository.RetrospectiveRepository;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class RetrospectiveApplicationConfiguration {

	@Bean
	@ConditionalOnBean(RetrospectiveRepository.class)
	RetrospectiveService retrospectiveService(
		RetrospectiveRepository repository,
		Clock clock,
		ObjectProvider<RetrospectiveFeedbackGenerator> feedbackGenerator,
		ObjectProvider<LlmUsageRecorder> usageRecorder,
		ObjectProvider<NotificationEventPublisher> notificationEvents,
		ObjectProvider<com.studypot.aistudyleader.report.service.WeeklyReportTrigger> weeklyReportTrigger
	) {
		RetrospectiveFeedbackGenerator generator = feedbackGenerator.getIfAvailable();
		LlmUsageRecorder recorder = usageRecorder.getIfAvailable();
		NotificationEventPublisher publisher = notificationEvents.getIfAvailable(NotificationEventPublisher::noop);
		RetrospectiveService service = (generator == null || recorder == null)
			? new RetrospectiveService(repository, clock, UuidV7::generate, null, null, publisher)
			: new RetrospectiveService(repository, clock, UuidV7::generate, generator, recorder, publisher);
		service.setWeeklyReportTrigger(weeklyReportTrigger);
		return service;
	}

	@Bean
	@Conditional(LlmProviderConfiguredCondition.class)
	RetrospectiveFeedbackGenerator retrospectiveFeedbackGenerator(
		LlmProviderClient provider,
		ObjectMapper objectMapper
	) {
		return new ProviderBackedRetrospectiveFeedbackGenerator(
			provider,
			objectMapper
		);
	}
}
