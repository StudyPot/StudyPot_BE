package com.studypot.aistudyleader.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.ai.repository.AiConversationRepository;
import com.studypot.aistudyleader.global.domain.UuidV7;
import com.studypot.aistudyleader.llm.service.LlmProviderConfiguredCondition;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import com.studypot.aistudyleader.studygroup.board.service.GroupBoardService;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class AiConversationApplicationConfiguration {

	@Bean
	@ConditionalOnBean(GroupBoardService.class)
	AiConversationBoardGateway aiConversationBoardGateway(GroupBoardService groupBoardService) {
		return new GroupBoardBackedAiConversationBoardGateway(groupBoardService);
	}

	@Bean
	@ConditionalOnBean(AiConversationRepository.class)
	AiConversationService aiConversationService(
		AiConversationRepository repository,
		Clock clock,
		ObjectProvider<AiConversationAssistantResponseGenerator> assistantResponseGenerator,
		ObjectProvider<LlmUsageRecorder> usageRecorder,
		ObjectProvider<AiConversationStreamPublisher> streamPublisher,
		ObjectProvider<AiConversationBoardGateway> boardGateway
	) {
		AiConversationAssistantResponseGenerator generator = assistantResponseGenerator.getIfAvailable();
		LlmUsageRecorder recorder = usageRecorder.getIfAvailable();
		AiConversationStreamPublisher publisher = streamPublisher.getIfAvailable(AiConversationStreamPublisher::noop);
		AiConversationBoardGateway gateway = boardGateway.getIfAvailable();
		if (generator == null || recorder == null) {
			return new AiConversationService(repository, clock, UuidV7::generate, null, null, publisher, gateway);
		}
		return new AiConversationService(repository, clock, UuidV7::generate, generator, recorder, publisher, gateway);
	}

	@Bean
	@Conditional(LlmProviderConfiguredCondition.class)
	@ConditionalOnMissingBean(AiConversationAssistantResponseGenerator.class)
	AiConversationAssistantResponseGenerator aiConversationAssistantResponseGenerator(
		LlmProviderClient provider,
		ObjectMapper objectMapper
	) {
		return new ProviderBackedAiConversationAssistantResponseGenerator(provider, objectMapper);
	}
}
