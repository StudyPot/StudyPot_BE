package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import java.util.Map;

interface OpenAiResponsesTransport {

	String createResponse(Map<String, Object> request);
}
