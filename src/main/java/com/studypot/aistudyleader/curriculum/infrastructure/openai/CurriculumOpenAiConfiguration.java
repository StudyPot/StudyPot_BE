package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.curriculum.service.ProviderBackedCurriculumGenerator;
import com.studypot.aistudyleader.curriculum.service.CurriculumGenerator;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpenAiCurriculumProperties.class)
class CurriculumOpenAiConfiguration {

	@Bean
	@Conditional(OpenAiApiKeyConfiguredCondition.class)
	OpenAiResponsesTransport openAiResponsesTransport(OpenAiCurriculumProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.connectTimeout());
		requestFactory.setReadTimeout(properties.readTimeout());
		RestClient restClient = RestClient.builder()
			.requestFactory(requestFactory)
			.baseUrl(properties.baseUrl())
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
			.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
			.build();
		return new RestClientOpenAiResponsesTransport(restClient);
	}

	@Bean
	@ConditionalOnBean(OpenAiResponsesTransport.class)
	@ConditionalOnMissingBean(LlmProviderClient.class)
	LlmProviderClient openAiLlmProvider(
		OpenAiResponsesTransport transport,
		ObjectMapper objectMapper,
		OpenAiCurriculumProperties properties
	) {
		return new OpenAiLlmProvider(
			transport,
			objectMapper,
			properties.model(),
			properties.apiMode(),
			properties.outputTokenLimits(),
			properties.models()
		);
	}

	@Bean
	@ConditionalOnBean(LlmProviderClient.class)
	@ConditionalOnMissingBean(CurriculumGenerator.class)
	CurriculumGenerator providerBackedCurriculumGenerator(
		LlmProviderClient provider,
		ObjectMapper objectMapper
	) {
		return new ProviderBackedCurriculumGenerator(provider, objectMapper);
	}
}
