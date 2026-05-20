package com.studypot.aistudyleader.studygroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsage;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.service.LlmCallFailure;
import com.studypot.aistudyleader.llm.service.LlmProviderCallException;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmStructuredRequest;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DetailKeywordSuggestionServiceTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000004501");
	private static final UUID USAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000004502");
	private static final Instant NOW = Instant.parse("2026-05-13T02:45:00Z");

	@Test
	void suggestReturnsProviderCandidatesAndRecordsUsageWithoutPersistingCandidates() {
		CapturingProvider provider = new CapturingProvider(new LlmStructuredResponse(
			LlmProvider.OPENAI,
			"gpt-4o-mini",
			"""
				{"keywords":["JPA 성능 최적화","Spring Security 인증"]}
				""",
			45,
			32,
			BigDecimal.ZERO,
			120,
			LlmUsageStatus.SUCCESS,
			null,
			Map.of("purpose", "DETAIL_KEYWORD_SUGGEST"),
			"raw provider response"
		));
		CapturingUsageRecorder usageRecorder = new CapturingUsageRecorder();
		DetailKeywordSuggestionService service = new DetailKeywordSuggestionService(
			provider,
			JsonMapper.builder().findAndAddModules().build(),
			usageRecorder,
			Clock.fixed(NOW, ZoneOffset.UTC),
			() -> USAGE_ID
		);

		DetailKeywordSuggestions result = service.suggest(new SuggestDetailKeywordsCommand(
			USER_ID,
			"Spring Boot",
			List.of("JPA", "Security"),
			3
		));

		assertThat(provider.request.purpose()).isEqualTo(LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST);
		assertThat(provider.request.input())
			.containsEntry("topic", "Spring Boot")
			.containsEntry("hintKeywords", List.of("JPA", "Security"))
			.containsEntry("maxCandidates", 3);
		assertThat(provider.request.textFormat().toString()).contains("detail_keyword_suggestions");
		assertThat(provider.request.textFormat().toString()).contains("keywords");
		assertThat(provider.request.textFormat().toString()).doesNotContain("reason");
		assertThat(result.keywords())
			.containsExactly("JPA 성능 최적화", "Spring Security 인증");
		assertThat(usageRecorder.usage.id()).isEqualTo(USAGE_ID);
		assertThat(usageRecorder.usage.userId()).isEqualTo(USER_ID);
		assertThat(usageRecorder.usage.groupId()).isNull();
		assertThat(usageRecorder.usage.purpose()).isEqualTo(LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST);
		assertThat(usageRecorder.usage.status()).isEqualTo(LlmUsageStatus.SUCCESS);
		assertThat(usageRecorder.usage.responseSummary()).isEqualTo("Generated detail keyword suggestions: 2");
	}

	@Test
	void suggestRecordsFailedUsageWhenProviderOutputIsMalformed() {
		CapturingUsageRecorder usageRecorder = new CapturingUsageRecorder();
		DetailKeywordSuggestionService service = service(
			new CapturingProvider(response("{not-json")),
			usageRecorder
		);

		assertThatThrownBy(() -> service.suggest(command()))
			.isInstanceOf(StudyGroupServiceUnavailableException.class)
			.hasCauseInstanceOf(JsonProcessingException.class);
		assertThat(usageRecorder.usage.status()).isEqualTo(LlmUsageStatus.FAILED);
		assertThat(usageRecorder.usage.errorCode()).isEqualTo("DETAIL_KEYWORD_RESPONSE_INVALID");
	}

	@Test
	void suggestRecordsFailedUsageWhenProviderCallFails() {
		CapturingUsageRecorder usageRecorder = new CapturingUsageRecorder();
		LlmCallFailure failure = new LlmCallFailure(
			LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST,
			LlmProvider.OPENAI,
			"gpt-4o-mini",
			0,
			0,
			BigDecimal.ZERO,
			500,
			LlmUsageStatus.FAILED,
			"OPENAI_REQUEST_FAILED",
			Map.of("purpose", "DETAIL_KEYWORD_SUGGEST"),
			"OpenAI request failed."
		);
		DetailKeywordSuggestionService service = service(new FailingProvider(failure), usageRecorder);

		assertThatThrownBy(() -> service.suggest(command()))
			.isInstanceOf(StudyGroupServiceUnavailableException.class)
			.hasCauseInstanceOf(LlmProviderCallException.class);
		assertThat(usageRecorder.usage.status()).isEqualTo(LlmUsageStatus.FAILED);
		assertThat(usageRecorder.usage.errorCode()).isEqualTo("OPENAI_REQUEST_FAILED");
	}

	@Test
	void suggestRejectsEmptyProviderSuggestionsWithFailedUsage() {
		CapturingUsageRecorder usageRecorder = new CapturingUsageRecorder();
		DetailKeywordSuggestionService service = service(
			new CapturingProvider(response("""
				{"keywords":[]}
				""")),
			usageRecorder
		);

		assertThatThrownBy(() -> service.suggest(command()))
			.isInstanceOf(StudyGroupServiceUnavailableException.class)
			.hasCauseInstanceOf(IllegalArgumentException.class);
		assertThat(usageRecorder.usage.status()).isEqualTo(LlmUsageStatus.FAILED);
		assertThat(usageRecorder.usage.errorCode()).isEqualTo("DETAIL_KEYWORD_RESPONSE_INVALID");
	}

	@Test
	void suggestRejectsBlankProviderKeywordWithFailedUsage() {
		CapturingUsageRecorder usageRecorder = new CapturingUsageRecorder();
		DetailKeywordSuggestionService service = service(
			new CapturingProvider(response("""
				{"keywords":["JPA"," "]}
				""")),
			usageRecorder
		);

		assertThatThrownBy(() -> service.suggest(command()))
			.isInstanceOf(StudyGroupServiceUnavailableException.class)
			.hasCauseInstanceOf(IllegalArgumentException.class);
		assertThat(usageRecorder.usage.status()).isEqualTo(LlmUsageStatus.FAILED);
		assertThat(usageRecorder.usage.errorCode()).isEqualTo("DETAIL_KEYWORD_RESPONSE_INVALID");
	}

	@Test
	void suggestCommandRejectsInvalidRequiredInput() {
		assertThatThrownBy(() -> new SuggestDetailKeywordsCommand(USER_ID, " ", List.of("JPA"), 3))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("topic must not be blank");
		assertThatThrownBy(() -> new SuggestDetailKeywordsCommand(USER_ID, "Spring Boot", List.of("JPA"), 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("maxCandidates must be between 1 and 10");
		assertThatThrownBy(() -> new SuggestDetailKeywordsCommand(null, "Spring Boot", List.of("JPA"), 3))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("authenticatedUserId must not be null");
	}

	private static DetailKeywordSuggestionService service(LlmProviderClient provider, CapturingUsageRecorder usageRecorder) {
		return new DetailKeywordSuggestionService(
			provider,
			JsonMapper.builder().findAndAddModules().build(),
			usageRecorder,
			Clock.fixed(NOW, ZoneOffset.UTC),
			() -> USAGE_ID
		);
	}

	private static SuggestDetailKeywordsCommand command() {
		return new SuggestDetailKeywordsCommand(USER_ID, "Spring Boot", List.of("JPA", "Security"), 3);
	}

	private static LlmStructuredResponse response(String outputText) {
		return new LlmStructuredResponse(
			LlmProvider.OPENAI,
			"gpt-4o-mini",
			outputText,
			45,
			32,
			BigDecimal.ZERO,
			120,
			LlmUsageStatus.SUCCESS,
			null,
			Map.of("purpose", "DETAIL_KEYWORD_SUGGEST"),
			"raw provider response"
		);
	}

	private static final class CapturingProvider implements LlmProviderClient {

		private final LlmStructuredResponse response;
		private LlmStructuredRequest request;

		private CapturingProvider(LlmStructuredResponse response) {
			this.response = response;
		}

		@Override
		public LlmStructuredResponse requestStructured(LlmStructuredRequest request) {
			this.request = request;
			return response;
		}
	}

	private static final class FailingProvider implements LlmProviderClient {

		private final LlmCallFailure failure;

		private FailingProvider(LlmCallFailure failure) {
			this.failure = failure;
		}

		@Override
		public LlmStructuredResponse requestStructured(LlmStructuredRequest request) {
			throw new LlmProviderCallException("OpenAI request failed.", failure);
		}
	}

	private static final class CapturingUsageRecorder implements LlmUsageRecorder {

		private LlmUsage usage;

		@Override
		public void record(LlmUsage usage) {
			this.usage = usage;
		}
	}
}
