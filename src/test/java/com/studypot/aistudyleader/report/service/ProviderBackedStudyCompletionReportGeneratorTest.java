package com.studypot.aistudyleader.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmStructuredRequest;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProviderBackedStudyCompletionReportGeneratorTest {

	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-0000000090a1");

	@Test
	void generateBuildsCompletionRequestAndParsesTitleBody() {
		CapturingProvider provider = new CapturingProvider(response("""
			{"title":"Spring Boot 스터디 수료 리포트","body":"4주간 팀은 JPA와 트랜잭션을 완주했어요. 수고했어요!"}
			"""));
		ProviderBackedStudyCompletionReportGenerator generator = new ProviderBackedStudyCompletionReportGenerator(
			provider,
			JsonMapper.builder().findAndAddModules().build()
		);

		WeeklyReportGeneration result = generator.generate(new StudyCompletionReportData(
			GROUP_ID,
			"Spring Boot",
			4,
			List.of(new MemberRetrospectiveSummary("현우", "전반적으로 흐름이 좋았다.")),
			List.of(new MemberTaskProgress("현우", 10, 12))
		));

		assertThat(provider.request.purpose()).isEqualTo(LlmUsagePurpose.WEEKLY_REPORT);
		assertThat(provider.request.textFormat().toString()).contains("study_completion_report");
		assertThat(provider.request.requestPayload())
			.containsEntry("purpose", "STUDY_COMPLETION_REPORT")
			.containsEntry("groupId", GROUP_ID.toString())
			.containsEntry("totalWeeks", 4);
		assertThat(result.content().title()).isEqualTo("Spring Boot 스터디 수료 리포트");
		assertThat(result.content().body()).contains("완주");
	}

	@Test
	void generateRejectsInvalidOutput() {
		CapturingProvider provider = new CapturingProvider(response("{\"title\":\"\"}"));
		ProviderBackedStudyCompletionReportGenerator generator = new ProviderBackedStudyCompletionReportGenerator(
			provider,
			JsonMapper.builder().findAndAddModules().build()
		);

		assertThatThrownBy(() -> generator.generate(new StudyCompletionReportData(
				GROUP_ID, "스터디", 1, List.of(), List.of(new MemberTaskProgress("현우", 1, 2))
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
			Map.of("purpose", "STUDY_COMPLETION_REPORT"),
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
