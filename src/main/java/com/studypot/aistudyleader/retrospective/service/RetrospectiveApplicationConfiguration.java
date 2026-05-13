package com.studypot.aistudyleader.retrospective.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.global.domain.UuidV7;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import com.studypot.aistudyleader.retrospective.repository.RetrospectiveRepository;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class RetrospectiveApplicationConfiguration {

	@Bean
	@ConditionalOnBean(RetrospectiveRepository.class)
	RetrospectiveService retrospectiveService(
		RetrospectiveRepository repository,
		Clock clock,
		ObjectProvider<RetrospectiveFeedbackGenerator> feedbackGenerator,
		ObjectProvider<LlmUsageRecorder> usageRecorder
	) {
		RetrospectiveFeedbackGenerator generator = feedbackGenerator.getIfAvailable();
		LlmUsageRecorder recorder = usageRecorder.getIfAvailable();
		if (generator == null || recorder == null) {
			return new RetrospectiveService(repository, clock, UuidV7::generate);
		}
		return new RetrospectiveService(repository, clock, UuidV7::generate, generator, recorder);
	}

	@Bean
	@ConditionalOnBean(LlmProviderClient.class)
	RetrospectiveFeedbackGenerator retrospectiveFeedbackGenerator(
		LlmProviderClient provider,
		ObjectProvider<ObjectMapper> objectMapper
	) {
		return new ProviderBackedRetrospectiveFeedbackGenerator(
			provider,
			objectMapper.getIfAvailable(ObjectMapper::new)
		);
	}
}
