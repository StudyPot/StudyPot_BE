package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import java.util.Map;
import java.util.Objects;
import org.springframework.web.client.RestClient;

class RestClientOpenAiResponsesTransport implements OpenAiResponsesTransport {

	private final RestClient restClient;

	RestClientOpenAiResponsesTransport(RestClient restClient) {
		this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
	}

	@Override
	public String createResponse(Map<String, Object> request) {
		return restClient.post()
			.uri("/responses")
			.body(request)
			.retrieve()
			.body(String.class);
	}
}
