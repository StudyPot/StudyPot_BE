package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import java.util.Objects;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class RestClientOpenAiResponsesTransport implements OpenAiResponsesTransport {

	private final RestClient restClient;

	RestClientOpenAiResponsesTransport(RestClient restClient) {
		this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
	}

	@Override
	public String createResponse(OpenAiResponseRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		try {
			String responseBody = restClient.post()
				.uri(request.path())
				.body(request.body())
				.retrieve()
				.body(String.class);
			if (responseBody == null || responseBody.isBlank()) {
				throw new OpenAiClientException("OpenAI API returned an empty body.");
			}
			return responseBody;
		} catch (RestClientException exception) {
			throw new OpenAiClientException("OpenAI API request failed.", exception);
		}
	}
}
