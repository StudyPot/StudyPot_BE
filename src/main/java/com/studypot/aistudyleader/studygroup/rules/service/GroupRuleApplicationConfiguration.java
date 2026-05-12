package com.studypot.aistudyleader.studygroup.rules.service;

import com.studypot.aistudyleader.global.domain.UuidV7;
import com.studypot.aistudyleader.studygroup.rules.repository.GroupRuleRepository;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class GroupRuleApplicationConfiguration {

	@Bean
	@ConditionalOnBean(GroupRuleRepository.class)
	GroupRuleService groupRuleService(GroupRuleRepository repository, Clock clock) {
		return new GroupRuleService(repository, clock, UuidV7::generate);
	}
}
