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
		assertThat(transport.request).containsEntry("max_output_tokens", 16_384);
		assertThat(transport.request.get("input").toString()).contains("Spring Boot");
		assertThat(transport.request.get("text").toString()).contains("json_schema");
		assertThat(result.provider()).isEqualTo(LlmProvider.OPENAI);
		assertThat(result.model()).isEqualTo("gpt-4o-mini");
		assertThat(result.outputText()).isEqualTo("{\"ok\":true}");
		assertThat(result.inputTokens()).isEqualTo(12);
		assertThat(result.outputTokens()).isEqualTo(7);
		assertThat(result.totalCostUsd()).isEqualByComparingTo(new BigDecimal("0.000006"));
		assertThat(result.status()).isEqualTo(LlmUsageStatus.SUCCESS);
		assertThat(result.requestPayload()).containsEntry("authorization", "Bearer secret");
		assertThat(result.requestPayload()).containsEntry("apiMode", "RESPONSES");
		assertThat(result.requestPayload()).containsEntry("outputBudget", 16_384);
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
		assertThat(transport.request).containsEntry("max_completion_tokens", 256);
		assertThat(transport.request.get("messages").toString()).contains("developer", "Answer in Korean", "user", "Spring Boot");
		assertThat(transport.request.get("response_format").toString()).contains("json_schema", "sample");
		assertThat(result.provider()).isEqualTo(LlmProvider.OPENAI);
		assertThat(result.model()).isEqualTo("gpt-5.2");
		assertThat(result.outputText()).isEqualTo("{\"ok\":true}");
		assertThat(result.inputTokens()).isEqualTo(11);
		assertThat(result.outputTokens()).isEqualTo(6);
		assertThat(result.totalCostUsd()).isEqualByComparingTo(new BigDecimal("0.000103"));
		assertThat(result.status()).isEqualTo(LlmUsageStatus.SUCCESS);
		assertThat(result.requestPayload()).containsEntry("apiMode", "CHAT_COMPLETIONS");
		assertThat(result.requestPayload()).containsEntry("outputBudget", 256);
	}

	@Test
	void requestStructuredUsesConfiguredOutputTokenLimitForPurpose() {
		CapturingTransport transport = new CapturingTransport("""
			{
			  "choices": [
			    {"message": {"role": "assistant", "content": "{\\"ok\\":true}"}}
			  ],
			  "usage": {"prompt_tokens": 11, "completion_tokens": 6}
			}
			""");
		OpenAiLlmProvider provider = new OpenAiLlmProvider(
			transport,
			JsonMapper.builder().findAndAddModules().build(),
			"gpt-5.2",
			OpenAiApiMode.CHAT_COMPLETIONS,
			new OpenAiOutputTokenLimits(111, 222, 333, 444, 555, 666)
		);

		LlmStructuredResponse result = provider.requestStructured(new LlmStructuredRequest(
			LlmUsagePurpose.TEAM_LEAD_CHAT,
			"Return JSON only.",
			Map.of("message", "이번 주 계획 도와줘"),
			Map.of("type", "json_schema", "name", "sample", "schema", Map.of("type", "object")),
			Map.of("purpose", "TEAM_LEAD_CHAT")
		));

		assertThat(transport.request).containsEntry("max_completion_tokens", 444);
		assertThat(result.requestPayload()).containsEntry("outputBudget", 444);
	}

	@Test
	void requestStructuredUsesPurposeSpecificModelForDetailKeywordOnly() {
		CapturingTransport transport = new CapturingTransport("""
			{
			  "choices": [
			    {"message": {"role": "assistant", "content": "{\\"ok\\":true}"}}
			  ],
			  "usage": {"prompt_tokens": 11, "completion_tokens": 6}
			}
			""");
		OpenAiLlmProvider provider = new OpenAiLlmProvider(
			transport,
			JsonMapper.builder().findAndAddModules().build(),
			"gpt-5.2",
			OpenAiApiMode.CHAT_COMPLETIONS,
			OpenAiOutputTokenLimits.defaults(),
			new OpenAiPurposeModels("gpt-5-nano", null, null, null, null)
		);

		LlmStructuredResponse detailResult = provider.requestStructured(new LlmStructuredRequest(
			LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST,
			"Return JSON only.",
			Map.of("topic", "Spring Boot"),
			Map.of("type", "json_schema", "name", "sample", "schema", Map.of("type", "object")),
			Map.of("purpose", "DETAIL_KEYWORD_SUGGEST")
		));

		assertThat(transport.request).containsEntry("model", "gpt-5-nano");
		assertThat(detailResult.model()).isEqualTo("gpt-5-nano");

		LlmStructuredResponse curriculumResult = provider.requestStructured(new LlmStructuredRequest(
			LlmUsagePurpose.CURRICULUM_GENERATE,
			"Return JSON only.",
			Map.of("topic", "Spring Boot"),
			Map.of("type", "json_schema", "name", "sample", "schema", Map.of("type", "object")),
			Map.of("purpose", "CURRICULUM_GENERATE")
		));

		assertThat(transport.request).containsEntry("model", "gpt-5.2");
		assertThat(curriculumResult.model()).isEqualTo("gpt-5.2");
	}

	@Test
	void requestStructuredUsesMinimalReasoningForGpt5DetailKeywordChatCompletions() {
		CapturingTransport transport = new CapturingTransport("""
			{
			  "choices": [
			    {"message": {"role": "assistant", "content": "{\\"keywords\\":[\\"JPA\\"]}"}, "finish_reason": "stop"}
			  ],
			  "usage": {"prompt_tokens": 11, "completion_tokens": 6}
			}
			""");
		OpenAiLlmProvider provider = new OpenAiLlmProvider(
			transport,
			JsonMapper.builder().findAndAddModules().build(),
			"gpt-5.2",
			OpenAiApiMode.CHAT_COMPLETIONS,
			OpenAiOutputTokenLimits.defaults(),
			new OpenAiPurposeModels("gpt-5-nano", null, null, null, null)
		);

		LlmStructuredResponse result = provider.requestStructured(new LlmStructuredRequest(
			LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST,
			"Return JSON only.",
			Map.of("topic", "Spring Boot"),
			Map.of("type", "json_schema", "name", "sample", "schema", Map.of("type", "object")),
			Map.of("purpose", "DETAIL_KEYWORD_SUGGEST")
		));

		assertThat(transport.request).containsEntry("reasoning_effort", "minimal");
		assertThat(result.requestPayload()).containsEntry("reasoningEffort", "minimal");
	}

	@Test
	void requestStructuredOmitsReasoningForNonGpt5ChatCompletions() {
		CapturingTransport transport = new CapturingTransport("""
			{
			  "choices": [
			    {"message": {"role": "assistant", "content": "{\\"keywords\\":[\\"JPA\\"]}"}, "finish_reason": "stop"}
			  ],
			  "usage": {"prompt_tokens": 11, "completion_tokens": 6}
			}
			""");
		OpenAiLlmProvider provider = new OpenAiLlmProvider(
			transport,
			JsonMapper.builder().findAndAddModules().build(),
			"gpt-4o-mini",
			OpenAiApiMode.CHAT_COMPLETIONS
		);

		LlmStructuredResponse result = provider.requestStructured(new LlmStructuredRequest(
			LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST,
			"Return JSON only.",
			Map.of("topic", "Spring Boot"),
			Map.of("type", "json_schema", "name", "sample", "schema", Map.of("type", "object")),
			Map.of("purpose", "DETAIL_KEYWORD_SUGGEST")
		));

		assertThat(transport.request).doesNotContainKey("reasoning_effort");
		assertThat(result.requestPayload()).doesNotContainKey("reasoningEffort");
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
					assertThat(providerException.failure().requestPayload()).containsEntry("apiMode", "RESPONSES");
					assertThat(providerException.failure().requestPayload()).containsEntry("outputBudget", 16_384);
				});
	}

	@Test
	void teamLeadChatModelDependsOnUserPlan() {
		CapturingTransport transport = new CapturingTransport("""
			{
			  "choices": [
			    {"message": {"role": "assistant", "content": "{\\"ok\\":true}"}}
			  ],
			  "usage": {"prompt_tokens": 11, "completion_tokens": 6}
			}
			""");
		OpenAiLlmProvider provider = new OpenAiLlmProvider(
			transport,
			JsonMapper.builder().findAndAddModules().build(),
			"gpt-5-nano",
			OpenAiApiMode.CHAT_COMPLETIONS,
			OpenAiOutputTokenLimits.defaults(),
			new OpenAiPurposeModels(null, null, null, "gpt-5-mini", null)
		);

		// FREE(또는 미지정) 채팅 → 기본 모델(nano).
		provider.requestStructured(new LlmStructuredRequest(
			LlmUsagePurpose.TEAM_LEAD_CHAT,
			"Return JSON only.",
			Map.of("topic", "Spring Boot"),
			Map.of("type", "json_schema", "name", "sample", "schema", Map.of("type", "object")),
			Map.of("purpose", "TEAM_LEAD_CHAT"),
			"FREE"
		));
		assertThat(transport.request).containsEntry("model", "gpt-5-nano");

		// PREMIUM 채팅 → 용도별 상위 모델(mini).
		provider.requestStructured(new LlmStructuredRequest(
			LlmUsagePurpose.TEAM_LEAD_CHAT,
			"Return JSON only.",
			Map.of("topic", "Spring Boot"),
			Map.of("type", "json_schema", "name", "sample", "schema", Map.of("type", "object")),
			Map.of("purpose", "TEAM_LEAD_CHAT"),
			"PREMIUM"
		));
		assertThat(transport.request).containsEntry("model", "gpt-5-mini");
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
