package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.service.LlmProviderCallException;
import com.studypot.aistudyleader.llm.service.LlmStructuredRequest;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenAiLlmProviderTest {

	@Test
	void requestStructuredMapsGenericRequestToOpenAiResponsesAndExtractsOutputText() {
		CapturingTransport transport = new CapturingTransport("""
			{
			  "output": [
			    {
			      "type": "message",
			      "content": [
			        {"type": "output_text", "text": "{\\"ok\\":true}"}
			      ]
			    }
			  ],
			  "usage": {"input_tokens": 12, "output_tokens": 7}
			}
			""");
		OpenAiLlmProvider provider = new OpenAiLlmProvider(
			transport,
			JsonMapper.builder().findAndAddModules().build(),
			"gpt-4o-mini"
		);

		LlmStructuredResponse result = provider.requestStructured(new LlmStructuredRequest(
			LlmUsagePurpose.CURRICULUM_GENERATE,
			"Return JSON only.",
			Map.of("topic", "Spring Boot"),
			Map.of("type", "json_schema", "name", "sample", "schema", Map.of("type", "object")),
			Map.of("purpose", "CURRICULUM_GENERATE", "authorization", "Bearer secret")
		));

		assertThat(transport.request).containsEntry("model", "gpt-4o-mini");
		assertThat(transport.path).isEqualTo("/responses");
		assertThat(transport.request).containsEntry("instructions", "Return JSON only.");
		assertThat(transport.request.get("input").toString()).contains("Spring Boot");
		assertThat(transport.request.get("text").toString()).contains("json_schema");
		assertThat(result.provider()).isEqualTo(LlmProvider.OPENAI);
		assertThat(result.model()).isEqualTo("gpt-4o-mini");
		assertThat(result.outputText()).isEqualTo("{\"ok\":true}");
		assertThat(result.inputTokens()).isEqualTo(12);
		assertThat(result.outputTokens()).isEqualTo(7);
		assertThat(result.totalCostUsd()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(result.status()).isEqualTo(LlmUsageStatus.SUCCESS);
		assertThat(result.requestPayload()).containsEntry("authorization", "Bearer secret");
	}

	@Test
	void requestStructuredMapsGenericRequestToOpenAiChatCompletionsAndExtractsMessageContent() {
		CapturingTransport transport = new CapturingTransport("""
			{
			  "choices": [
			    {
			      "message": {
			        "role": "assistant",
			        "content": "{\\"ok\\":true}"
			      }
			    }
			  ],
			  "usage": {"prompt_tokens": 11, "completion_tokens": 6}
			}
			""");
		OpenAiLlmProvider provider = new OpenAiLlmProvider(
			transport,
			JsonMapper.builder().findAndAddModules().build(),
			"gpt-5.2",
			OpenAiApiMode.CHAT_COMPLETIONS
		);

		LlmStructuredResponse result = provider.requestStructured(new LlmStructuredRequest(
			LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST,
			"Answer in Korean and return JSON only.",
			Map.of("topic", "Spring Boot"),
			Map.of("type", "json_schema", "name", "sample", "strict", true, "schema", Map.of("type", "object")),
			Map.of("purpose", "DETAIL_KEYWORD_SUGGEST")
		));

		assertThat(transport.path).isEqualTo("/chat/completions");
		assertThat(transport.request).containsEntry("model", "gpt-5.2");
		assertThat(transport.request).containsKey("messages");
		assertThat(transport.request.get("messages").toString()).contains("developer", "Answer in Korean", "user", "Spring Boot");
		assertThat(transport.request.get("response_format").toString()).contains("json_schema", "sample");
		assertThat(result.provider()).isEqualTo(LlmProvider.OPENAI);
		assertThat(result.model()).isEqualTo("gpt-5.2");
		assertThat(result.outputText()).isEqualTo("{\"ok\":true}");
		assertThat(result.inputTokens()).isEqualTo(11);
		assertThat(result.outputTokens()).isEqualTo(6);
		assertThat(result.status()).isEqualTo(LlmUsageStatus.SUCCESS);
	}

	@Test
	void requestStructuredThrowsFailureWithAuditMetadataWhenOpenAiResponseHasNoOutputText() {
		OpenAiLlmProvider provider = new OpenAiLlmProvider(
			new CapturingTransport("""
				{"output": [], "usage": {"input_tokens": 12, "output_tokens": 0}}
				"""),
			JsonMapper.builder().findAndAddModules().build(),
			"gpt-4o-mini"
		);

		assertThatThrownBy(() -> provider.requestStructured(new LlmStructuredRequest(
				LlmUsagePurpose.CURRICULUM_GENERATE,
				"Return JSON only.",
				Map.of("topic", "Spring Boot"),
				Map.of("type", "json_schema", "name", "sample", "schema", Map.of("type", "object")),
				Map.of("purpose", "CURRICULUM_GENERATE")
			)))
			.isInstanceOf(LlmProviderCallException.class)
			.satisfies(exception -> {
				LlmProviderCallException providerException = (LlmProviderCallException) exception;
				assertThat(providerException.failure().purpose()).isEqualTo(LlmUsagePurpose.CURRICULUM_GENERATE);
				assertThat(providerException.failure().provider()).isEqualTo(LlmProvider.OPENAI);
				assertThat(providerException.failure().status()).isEqualTo(LlmUsageStatus.FAILED);
				assertThat(providerException.failure().errorCode()).isEqualTo("OPENAI_OUTPUT_TEXT_MISSING");
			});
	}

	private static final class CapturingTransport implements OpenAiResponsesTransport {

		private final String response;
		private String path;
		private Map<String, Object> request;

		private CapturingTransport(String response) {
			this.response = response;
		}

		@Override
		public String createResponse(OpenAiResponseRequest request) {
			this.path = request.path();
			this.request = request.body();
			return response;
		}
	}
}
