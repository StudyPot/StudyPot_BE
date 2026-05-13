package com.studypot.aistudyleader.ai.service;

import com.studypot.aistudyleader.ai.repository.AiConversationRepository;
import com.studypot.aistudyleader.global.domain.UuidV7;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class AiConversationApplicationConfiguration {

	@Bean
	@ConditionalOnBean(AiConversationRepository.class)
	AiConversationService aiConversationService(AiConversationRepository repository, Clock clock) {
		return new AiConversationService(repository, clock, UuidV7::generate);
	}
}
