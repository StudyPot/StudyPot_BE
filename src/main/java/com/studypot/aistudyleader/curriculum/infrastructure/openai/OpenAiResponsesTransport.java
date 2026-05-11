package com.studypot.aistudyleader.curriculum.infrastructure.openai;

public interface OpenAiResponsesTransport {

	String createResponse(OpenAiResponseRequest request) throws OpenAiClientException;
}
