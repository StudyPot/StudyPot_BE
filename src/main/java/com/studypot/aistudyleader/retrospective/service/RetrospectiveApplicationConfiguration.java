package com.studypot.aistudyleader.retrospective.service;

import com.studypot.aistudyleader.global.domain.UuidV7;
import com.studypot.aistudyleader.retrospective.repository.RetrospectiveRepository;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class RetrospectiveApplicationConfiguration {

	@Bean
	@ConditionalOnBean(RetrospectiveRepository.class)
	RetrospectiveService retrospectiveService(RetrospectiveRepository repository, Clock clock) {
		return new RetrospectiveService(repository, clock, UuidV7::generate);
	}
}
