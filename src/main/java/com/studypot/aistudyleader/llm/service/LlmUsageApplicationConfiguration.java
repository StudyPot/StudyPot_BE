package com.studypot.aistudyleader.llm.service;

import com.studypot.aistudyleader.llm.repository.LlmUsageRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class LlmUsageApplicationConfiguration {

	@Bean
	@ConditionalOnBean(LlmUsageRepository.class)
	LlmUsageService llmUsageService(LlmUsageRepository repository) {
		return new LlmUsageService(repository);
	}

	@Bean
	@ConditionalOnBean(LlmUsageRepository.class)
	LlmUsageRecorder llmUsageRecorder(LlmUsageRepository repository) {
		return usage -> {
			if (!repository.insertLlmUsage(usage)) {
				throw new LlmUsageServiceUnavailableException("LLM usage could not be recorded.");
			}
		};
	}
}
