package com.studypot.aistudyleader.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.service.LlmCallFailure;
import com.studypot.aistudyleader.llm.service.LlmProviderCallException;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmStructuredRequest;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProviderBackedWeeklyReportGeneratorTest {

	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000009001");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000009002");

	@Test
	void generateBuildsReportRequestAndParsesTitleBody() {
		CapturingProvider provider = new CapturingProvider(response("""
			{"title":"2주차 학습 리포트","body":"이번 주 팀은 JPA 실습을 잘 마쳤습니다."}
			"""));
		ProviderBackedWeeklyReportGenerator generator = new ProviderBackedWeeklyReportGenerator(
			provider,
			JsonMapper.builder().findAndAddModules().build()
		);

		WeeklyReportGeneration result = generator.generate(new WeeklyReportData(
			GROUP_ID,
			WEEK_ID,
			2,
			"JPA 심화",
			List.of(
				new MemberRetrospectiveSummary("현우", "실습을 끝냈고 흐름이 좋다."),
				new MemberRetrospectiveSummary("지민", "이론은 좋았으나 실습이 부족했다.")
			),
			List.of(new MemberTaskProgress("현우", 3, 4))
		));

		assertThat(provider.request.purpose()).isEqualTo(LlmUsagePurpose.WEEKLY_REPORT);
		assertThat(provider.request.textFormat().toString()).contains("weekly_report");
		assertThat(provider.request.requestPayload())
			.containsEntry("purpose", "WEEKLY_REPORT")
			.containsEntry("weekId", WEEK_ID.toString())
			.containsEntry("memberCount", 2);
		assertThat(result.content().title()).isEqualTo("2주차 학습 리포트");
		assertThat(result.content().body()).isEqualTo("이번 주 팀은 JPA 실습을 잘 마쳤습니다.");
		assertThat(result.response().responseSummary()).isEqualTo("Generated weekly report: 2주차 학습 리포트");
	}

	@Test
	void generateRejectsInvalidOutput() {
		CapturingProvider provider = new CapturingProvider(response("{\"title\":\"\"}"));
		ProviderBackedWeeklyReportGenerator generator = new ProviderBackedWeeklyReportGenerator(
			provider,
			JsonMapper.builder().findAndAddModules().build()
		);

		assertThatThrownBy(() -> generator.generate(new WeeklyReportData(
				GROUP_ID, WEEK_ID, 1, "1주차", List.of(new MemberRetrospectiveSummary("현우", "좋았다.")), List.of()
			)))
			.isInstanceOf(WeeklyReportGenerationException.class);
	}

	@Test
	void generateWrapsProviderCallFailure() {
		LlmCallFailure failure = new LlmCallFailure(
			LlmUsagePurpose.WEEKLY_REPORT, LlmProvider.OPENAI, "gpt-4o-mini",
			0, 0, BigDecimal.ZERO, 0, LlmUsageStatus.FAILED, "PROVIDER_ERROR",
			Map.of("purpose", "WEEKLY_REPORT"), null
		);
		LlmProviderClient provider = request -> {
			throw new LlmProviderCallException("boom", failure);
		};
		ProviderBackedWeeklyReportGenerator generator = new ProviderBackedWeeklyReportGenerator(
			provider,
			JsonMapper.builder().findAndAddModules().build()
		);

		assertThatThrownBy(() -> generator.generate(new WeeklyReportData(
				GROUP_ID, WEEK_ID, 1, "1주차", List.of(new MemberRetrospectiveSummary("현우", "좋았다.")), List.of()
			)))
			.isInstanceOf(WeeklyReportGenerationException.class);
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
			Map.of("purpose", "WEEKLY_REPORT"),
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
