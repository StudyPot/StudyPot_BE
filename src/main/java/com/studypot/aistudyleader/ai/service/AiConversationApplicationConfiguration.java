package com.studypot.aistudyleader.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.ai.repository.AiConversationRepository;
import com.studypot.aistudyleader.global.domain.UuidV7;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class AiConversationApplicationConfiguration {

	@Bean
	@ConditionalOnBean(AiConversationRepository.class)
	AiConversationService aiConversationService(
		AiConversationRepository repository,
		Clock clock,
		ObjectProvider<AiConversationAssistantResponseGenerator> assistantResponseGenerator,
		ObjectProvider<LlmUsageRecorder> usageRecorder
	) {
		AiConversationAssistantResponseGenerator generator = assistantResponseGenerator.getIfAvailable();
		LlmUsageRecorder recorder = usageRecorder.getIfAvailable();
		if (generator == null || recorder == null) {
			return new AiConversationService(repository, clock, UuidV7::generate);
		}
		return new AiConversationService(repository, clock, UuidV7::generate, generator, recorder);
	}

	@Bean
	@ConditionalOnBean(LlmProviderClient.class)
	@ConditionalOnMissingBean(AiConversationAssistantResponseGenerator.class)
	AiConversationAssistantResponseGenerator aiConversationAssistantResponseGenerator(
		LlmProviderClient provider,
		ObjectMapper objectMapper
	) {
		return new ProviderBackedAiConversationAssistantResponseGenerator(provider, objectMapper);
	}
}
