package com.studypot.aistudyleader.follow.service;

import com.studypot.aistudyleader.follow.repository.FollowRepository;
import com.studypot.aistudyleader.global.domain.UuidV7;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class FollowApplicationConfiguration {

	@Bean
	@ConditionalOnBean({FollowRepository.class, Clock.class})
	FollowService followService(FollowRepository repository, Clock clock) {
		return new FollowService(repository, clock, UuidV7::generate);
	}
}
