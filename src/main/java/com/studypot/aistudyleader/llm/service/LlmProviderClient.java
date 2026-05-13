package com.studypot.aistudyleader.llm.service;

public interface LlmProviderClient {

	LlmStructuredResponse requestStructured(LlmStructuredRequest request);
}
