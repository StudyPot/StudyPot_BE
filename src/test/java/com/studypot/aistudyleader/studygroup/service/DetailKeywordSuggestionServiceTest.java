package com.studypot.aistudyleader.studygroup.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsage;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
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
				{"suggestions":[{"keyword":"JPA 성능 최적화","reason":"Spring Boot 주제와 기존 힌트를 더 세분화합니다."},{"keyword":"Spring Security 인증","reason":"보안 학습 범위를 명확히 합니다."}],"rationale":"호스트가 선택할 수 있는 세부 키워드 후보입니다."}
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
		assertThat(result.suggestions()).extracting(DetailKeywordSuggestion::keyword)
			.containsExactly("JPA 성능 최적화", "Spring Security 인증");
		assertThat(result.rationale()).isEqualTo("호스트가 선택할 수 있는 세부 키워드 후보입니다.");
		assertThat(usageRecorder.usage.id()).isEqualTo(USAGE_ID);
		assertThat(usageRecorder.usage.userId()).isEqualTo(USER_ID);
		assertThat(usageRecorder.usage.groupId()).isNull();
		assertThat(usageRecorder.usage.purpose()).isEqualTo(LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST);
		assertThat(usageRecorder.usage.status()).isEqualTo(LlmUsageStatus.SUCCESS);
		assertThat(usageRecorder.usage.responseSummary()).isEqualTo("Generated detail keyword suggestions: 2");
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

	private static final class CapturingUsageRecorder implements LlmUsageRecorder {

		private LlmUsage usage;

		@Override
		public void record(LlmUsage usage) {
			this.usage = usage;
		}
	}
}
