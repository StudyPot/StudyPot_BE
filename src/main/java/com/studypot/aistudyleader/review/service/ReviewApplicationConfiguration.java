package com.studypot.aistudyleader.review.service;

import com.studypot.aistudyleader.global.domain.UuidV7;
import com.studypot.aistudyleader.review.repository.ReviewRepository;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ReviewApplicationConfiguration {

	@Bean
	@ConditionalOnBean(ReviewRepository.class)
	ReviewService reviewService(ReviewRepository repository, Clock clock) {
		return new ReviewService(repository, clock, UuidV7::generate);
	}
}
