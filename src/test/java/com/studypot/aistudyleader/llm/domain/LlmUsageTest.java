package com.studypot.aistudyleader.llm.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LlmUsageTest {

	private static final UUID USAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000007001");
	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000007002");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000007003");
	private static final Instant NOW = Instant.parse("2026-05-13T02:10:00Z");

	@Test
	void purposeValuesCoverJiraAndLockedAiContract() {
		assertThat(Arrays.stream(LlmUsagePurpose.values()).map(Enum::name))
			.containsExactly(
				"DETAIL_KEYWORD_SUGGEST",
				"CURRICULUM_GENERATE",
				"CURRICULUM_REGENERATE_WEEK",
				"TEAM_LEAD_CHAT",
				"RETROSPECTIVE_ANALYZE",
				"RETROSPECTIVE_FEEDBACK",
				"NEXT_WEEK_ADJUST"
			);
	}

	@Test
	void recordsUsageAndRedactsRequestPayload() {
		String longPrivateNote = "개인 메모".repeat(120);

		LlmUsage usage = usage(Map.of(
			"authorization", "Bearer raw-provider-token",
			"context", Map.of(
				"safeSource", "weekly_progress",
				"oauthRefreshToken", "raw-refresh-token",
				"privateNote", longPrivateNote
			)
		));

		assertThat(usage.id()).isEqualTo(USAGE_ID);
		assertThat(usage.purpose()).isEqualTo(LlmUsagePurpose.TEAM_LEAD_CHAT);
		assertThat(usage.provider()).isEqualTo(LlmProvider.OPENAI);
		assertThat(usage.model()).isEqualTo("gpt-4.1-mini");
		assertThat(usage.inputTokens()).isEqualTo(120);
		assertThat(usage.outputTokens()).isEqualTo(45);
		assertThat(usage.totalCostUsd()).isEqualByComparingTo("0.000321");
		assertThat(usage.latencyMs()).isEqualTo(230);
		assertThat(usage.status()).isEqualTo(LlmUsageStatus.SUCCESS);
		assertThat(usage.createdDateUtc()).isEqualTo(LocalDate.of(2026, 5, 13));
		assertThat(usage.requestPayload()).containsEntry("authorization", "[REDACTED]");
		@SuppressWarnings("unchecked")
		Map<String, Object> context = (Map<String, Object>) usage.requestPayload().get("context");
		assertThat(context)
			.containsEntry("safeSource", "weekly_progress")
			.containsEntry("oauthRefreshToken", "[REDACTED]");
		assertThat((String) context.get("privateNote"))
			.endsWith("...[TRUNCATED]")
			.hasSizeLessThan(600);
	}

	@Test
	void rejectsInvalidMetrics() {
		assertThatThrownBy(() -> usage(-1, 0, BigDecimal.ZERO, 10))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("token counts must not be negative.");
		assertThatThrownBy(() -> usage(0, 0, new BigDecimal("-0.01"), 10))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("totalCostUsd must not be negative.");
		assertThatThrownBy(() -> usage(0, 0, BigDecimal.ZERO, -1))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("latencyMs must not be negative.");
	}

	@Test
	void keepsNullValuesInRequestPayloadWithoutLeakingSecrets() {
		Map<String, Object> requestPayload = new LinkedHashMap<>();
		requestPayload.put("optionalContext", null);
		requestPayload.put("headers", List.of(Map.of("apiKey", "sk-raw"), "safe"));

		LlmUsage usage = usage(requestPayload);

		assertThat(usage.requestPayload()).containsEntry("optionalContext", null);
		@SuppressWarnings("unchecked")
		List<Object> headers = (List<Object>) usage.requestPayload().get("headers");
		@SuppressWarnings("unchecked")
		Map<String, Object> firstHeader = (Map<String, Object>) headers.getFirst();
		assertThat(firstHeader).containsEntry("apiKey", "[REDACTED]");
	}

	private static LlmUsage usage(Map<String, Object> requestPayload) {
		return usage(120, 45, new BigDecimal("0.000321"), 230, requestPayload);
	}

	private static LlmUsage usage(int inputTokens, int outputTokens, BigDecimal totalCostUsd, Integer latencyMs) {
		return usage(inputTokens, outputTokens, totalCostUsd, latencyMs, Map.of());
	}

	private static LlmUsage usage(
		int inputTokens,
		int outputTokens,
		BigDecimal totalCostUsd,
		Integer latencyMs,
		Map<String, Object> requestPayload
	) {
		return LlmUsage.record(
			USAGE_ID,
			USER_ID,
			GROUP_ID,
			LlmUsagePurpose.TEAM_LEAD_CHAT,
			LlmProvider.OPENAI,
			"gpt-4.1-mini",
			inputTokens,
			outputTokens,
			totalCostUsd,
			latencyMs,
			LlmUsageStatus.SUCCESS,
			null,
			requestPayload,
			"assistant response summary",
			NOW
		);
	}
}
