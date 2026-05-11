package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.curriculum.service.CurriculumGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpenAiCurriculumProperties.class)
class CurriculumOpenAiConfiguration {

	@Bean
	@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${studypot.ai.openai.api-key:}')")
	OpenAiResponsesTransport openAiResponsesTransport(OpenAiCurriculumProperties properties) {
		RestClient restClient = RestClient.builder()
			.baseUrl(properties.baseUrl())
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
			.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
			.build();
		return new RestClientOpenAiResponsesTransport(restClient);
	}

	@Bean
	@ConditionalOnBean(OpenAiResponsesTransport.class)
	@ConditionalOnMissingBean(CurriculumGenerator.class)
	CurriculumGenerator openAiCurriculumGenerator(
		OpenAiResponsesTransport transport,
		ObjectMapper objectMapper,
		OpenAiCurriculumProperties properties
	) {
		return new OpenAiCurriculumGenerator(transport, objectMapper, properties.model());
	}
}
