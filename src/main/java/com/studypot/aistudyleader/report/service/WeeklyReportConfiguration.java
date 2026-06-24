package com.studypot.aistudyleader.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmProviderConfiguredCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class WeeklyReportConfiguration {

	@Bean
	@Conditional(LlmProviderConfiguredCondition.class)
	WeeklyReportGenerator weeklyReportGenerator(LlmProviderClient provider, ObjectMapper objectMapper) {
		return new ProviderBackedWeeklyReportGenerator(provider, objectMapper);
	}

	@Bean
	@Conditional(LlmProviderConfiguredCondition.class)
	StudyCompletionReportGenerator studyCompletionReportGenerator(LlmProviderClient provider, ObjectMapper objectMapper) {
		return new ProviderBackedStudyCompletionReportGenerator(provider, objectMapper);
	}
}
