package com.studypot.aistudyleader.onboarding.service;

import com.studypot.aistudyleader.global.domain.UuidV7;
import com.studypot.aistudyleader.onboarding.repository.OnboardingRepository;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OnboardingApplicationConfiguration {

	@Bean
	@ConditionalOnBean(OnboardingRepository.class)
	OnboardingService onboardingService(OnboardingRepository repository, Clock clock) {
		return new OnboardingService(repository, clock, UuidV7::generate);
	}
}
