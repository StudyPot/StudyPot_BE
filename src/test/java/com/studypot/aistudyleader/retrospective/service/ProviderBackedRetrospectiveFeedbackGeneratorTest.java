package com.studypot.aistudyleader.retrospective.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmStructuredRequest;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import com.studypot.aistudyleader.retrospective.domain.Retrospective;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveStatus;
import com.studypot.aistudyleader.retrospective.domain.RetrospectiveTriggerType;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProviderBackedRetrospectiveFeedbackGeneratorTest {

	private static final UUID RETROSPECTIVE_ID = UUID.fromString("018f0000-0000-7000-8000-000000006901");
	private static final UUID PROGRESS_ID = UUID.fromString("018f0000-0000-7000-8000-000000006902");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000006903");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000006904");
	private static final Instant NOW = Instant.parse("2026-05-13T03:10:00Z");
	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();
	private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
	};

	@Test
	void generateUsesRetrospectiveContextAndParsesFeedbackJson() {
		CapturingProvider provider = new CapturingProvider(response("""
			{"summary":"실습 시간이 부족했습니다.","strengths":["읽기 완료"],"risks":["실습 지연"],"actionItems":["실습량 조정"],"nextWeekAdjustment":{"difficulty":"slightly_lower","taskChanges":["필수 실습 분리"],"supportMaterials":["기초 예제"],"memberNotes":[{"memberId":"018f0000-0000-7000-8000-000000006904","note":"실습 시간을 먼저 확보"}]}}
			"""));
		ProviderBackedRetrospectiveFeedbackGenerator generator = new ProviderBackedRetrospectiveFeedbackGenerator(
			provider,
			JsonMapper.builder().findAndAddModules().build()
		);

		RetrospectiveFeedbackGeneration result = generator.generate(retrospective());

		assertThat(provider.request.purpose()).isEqualTo(LlmUsagePurpose.RETROSPECTIVE_FEEDBACK);
		assertThat(provider.request.instructions()).contains("retrospective feedback");
		assertThat(provider.request.input()).containsEntry("retrospectiveId", RETROSPECTIVE_ID.toString());
		assertThat(provider.request.input()).containsKey("context");
		assertThat(provider.request.textFormat().toString()).contains("retrospective_feedback");
		assertThat(provider.request.requestPayload())
			.containsEntry("purpose", "RETROSPECTIVE_FEEDBACK")
			.containsEntry("retrospectiveId", RETROSPECTIVE_ID.toString())
			.containsEntry("weekId", WEEK_ID.toString())
			.containsEntry("memberId", MEMBER_ID.toString())
			.containsEntry("taskCount", 1)
			.containsEntry("ruleViolationCount", 1);
		assertThat(result.feedbackResult().aiFeedback())
			.containsEntry("summary", "실습 시간이 부족했습니다.")
			.containsEntry("strengths", List.of("읽기 완료"))
			.containsEntry("risks", List.of("실습 지연"))
			.containsEntry("actionItems", List.of("실습량 조정"));
		assertThat(result.feedbackResult().nextWeekAdjustment()).containsEntry("difficulty", "slightly_lower");
		assertThat(result.response().responseSummary()).isEqualTo("Generated retrospective feedback summary: 실습 시간이 부족했습니다.");
	}

	@Test
	void goldenContextKeepsRequiredSourcesAndRedactsCredentialLikeValuesBeforeProviderCall() {
		CapturingProvider provider = new CapturingProvider(response("""
			{"summary":"실습 시간이 부족했습니다.","strengths":["읽기 완료"],"risks":["실습 지연"],"actionItems":["실습량 조정"],"nextWeekAdjustment":{"difficulty":"slightly_lower"}}
			"""));
		ProviderBackedRetrospectiveFeedbackGenerator generator = new ProviderBackedRetrospectiveFeedbackGenerator(
			provider,
			OBJECT_MAPPER
		);

		generator.generate(retrospective(readFixture("/ai-context/retrospective-feedback-context-golden.json")));

		String providerInput = provider.request.input().toString();
		assertThat(providerInput)
			.contains("progress")
			.contains("taskCompletionCounts")
			.contains("tasks")
			.contains("onboarding")
			.contains("rules")
			.contains("ruleViolations")
			.contains("priorRetrospectives")
			.contains("conversationSummary")
			.contains("completionNote")
			.contains("incompleteReason")
			.contains("JPA 실습")
			.contains("[REDACTED]")
			.doesNotContain("raw-")
			.doesNotContain("Bearer raw")
			.doesNotContain("SESSION=raw");
		assertThat(provider.request.requestPayload().toString()).doesNotContain("raw-");
	}

	@Test
	void generateRejectsInvalidFeedbackJsonWithFailureMetadata() {
		ProviderBackedRetrospectiveFeedbackGenerator generator = new ProviderBackedRetrospectiveFeedbackGenerator(
			new CapturingProvider(response("{not-json")),
			JsonMapper.builder().findAndAddModules().build()
		);

		assertThatThrownBy(() -> generator.generate(retrospective()))
			.isInstanceOf(RetrospectiveFeedbackGenerationException.class)
			.hasMessage("retrospective feedback output was invalid.")
			.hasCauseInstanceOf(JsonProcessingException.class)
			.satisfies(exception -> {
				RetrospectiveFeedbackGenerationException generationException = (RetrospectiveFeedbackGenerationException) exception;
				assertThat(generationException.failure().purpose()).isEqualTo(LlmUsagePurpose.RETROSPECTIVE_FEEDBACK);
				assertThat(generationException.failure().status()).isEqualTo(LlmUsageStatus.FAILED);
				assertThat(generationException.failure().errorCode()).isEqualTo("RETROSPECTIVE_RESPONSE_INVALID");
			});
	}

	@Test
	void generateRejectsMissingNextWeekAdjustmentWithFailureMetadata() {
		ProviderBackedRetrospectiveFeedbackGenerator generator = new ProviderBackedRetrospectiveFeedbackGenerator(
			new CapturingProvider(response("""
				{"summary":"실습 시간이 부족했습니다.","strengths":["읽기 완료"],"risks":["실습 지연"],"actionItems":["실습량 조정"]}
				""")),
			JsonMapper.builder().findAndAddModules().build()
		);

		assertThatThrownBy(() -> generator.generate(retrospective()))
			.isInstanceOf(RetrospectiveFeedbackGenerationException.class)
			.hasMessage("retrospective feedback output was invalid.")
			.hasCauseInstanceOf(IllegalArgumentException.class)
			.satisfies(exception -> {
				RetrospectiveFeedbackGenerationException generationException = (RetrospectiveFeedbackGenerationException) exception;
				assertThat(generationException.failure().purpose()).isEqualTo(LlmUsagePurpose.RETROSPECTIVE_FEEDBACK);
				assertThat(generationException.failure().status()).isEqualTo(LlmUsageStatus.FAILED);
				assertThat(generationException.failure().errorCode()).isEqualTo("RETROSPECTIVE_RESPONSE_INVALID");
			});
	}

	private static Retrospective retrospective() {
		return retrospective(Map.of(
			"progress", Map.of("status", "INCOMPLETE", "incompleteReason", "실습 시간 부족"),
			"tasks", List.of(Map.of("status", "INCOMPLETE", "title", "JPA 실습")),
			"ruleViolations", List.of(Map.of("status", "OPEN"))
		));
	}

	private static Retrospective retrospective(Map<String, Object> inputSummary) {
		return new Retrospective(
			RETROSPECTIVE_ID,
			PROGRESS_ID,
			WEEK_ID,
			MEMBER_ID,
			null,
			RetrospectiveTriggerType.MANUAL,
			inputSummary,
			Map.of(),
			Map.of(),
			RetrospectiveStatus.PROCESSING,
			NOW,
			null,
			NOW,
			NOW
		);
	}

	private static Map<String, Object> readFixture(String path) {
		try (InputStream input = ProviderBackedRetrospectiveFeedbackGeneratorTest.class.getResourceAsStream(path)) {
			assertThat(input).as(path).isNotNull();
			return OBJECT_MAPPER.readValue(input, OBJECT_MAP);
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	private static LlmStructuredResponse response(String outputText) {
		return new LlmStructuredResponse(
			LlmProvider.OPENAI,
			"gpt-4o-mini",
			outputText,
			80,
			60,
			BigDecimal.ZERO,
			120,
			LlmUsageStatus.SUCCESS,
			null,
			Map.of("purpose", "RETROSPECTIVE_FEEDBACK"),
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
}
