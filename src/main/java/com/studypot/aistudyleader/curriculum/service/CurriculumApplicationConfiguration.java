package com.studypot.aistudyleader.curriculum.service;

import com.studypot.aistudyleader.curriculum.repository.CurriculumRepository;
import com.studypot.aistudyleader.global.domain.UuidV7;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class CurriculumApplicationConfiguration {

	@Bean
	@ConditionalOnBean({CurriculumRepository.class, CurriculumGenerator.class})
	CurriculumService curriculumService(CurriculumRepository repository, CurriculumGenerator generator, Clock clock) {
		return new CurriculumService(repository, generator, clock, UuidV7::generate);
	}
}
