package com.studypot.aistudyleader.curriculum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmStructuredRequest;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProviderBackedNextWeekPlanGeneratorTest {

	@Test
	void generateBuildsRequestAndParsesTasksAndPrompt() {
		CapturingProvider provider = new CapturingProvider(response("""
			{"tasks":[{"taskType":"PRACTICE","title":"연관관계 매핑 실습","description":"OneToMany 실습","required":true}],"retrospectiveQuestions":[{"text":"이번 주 실습을 충분히 했다","type":"LIKERT_5"},{"text":"막힌 점은?","type":"TEXT"}]}
			"""));
		ProviderBackedNextWeekPlanGenerator generator = new ProviderBackedNextWeekPlanGenerator(
			provider, JsonMapper.builder().findAndAddModules().build()
		);

		NextWeekPlanGeneration result = generator.generate(new NextWeekPlanInput(2, "JPA 심화", "연관관계 정복", "지난 주 리포트 본문"));

		assertThat(provider.request.purpose()).isEqualTo(LlmUsagePurpose.NEXT_WEEK_ADJUST);
		assertThat(provider.request.textFormat().toString()).contains("next_week_plan");
		assertThat(provider.request.input()).containsEntry("nextWeekNumber", 2);
		assertThat(result.plan().tasks()).hasSize(1);
		assertThat(result.plan().tasks().getFirst().taskType()).isEqualTo(WeeklyTaskType.PRACTICE);
		assertThat(result.plan().retrospectiveQuestions()).hasSize(2);
		assertThat(result.plan().retrospectiveQuestions().getFirst().type())
			.isEqualTo(com.studypot.aistudyleader.curriculum.domain.RetrospectiveQuestionType.LIKERT_5);
	}

	@Test
	void generateRejectsEmptyTasks() {
		CapturingProvider provider = new CapturingProvider(response("{\"tasks\":[],\"retrospectiveQuestions\":[{\"text\":\"x\",\"type\":\"TEXT\"}]}"));
		ProviderBackedNextWeekPlanGenerator generator = new ProviderBackedNextWeekPlanGenerator(
			provider, JsonMapper.builder().findAndAddModules().build()
		);

		assertThatThrownBy(() -> generator.generate(new NextWeekPlanInput(2, "t", "g", "report")))
			.isInstanceOf(NextWeekPlanGenerationException.class);
	}

	private static LlmStructuredResponse response(String outputText) {
		return new LlmStructuredResponse(
			LlmProvider.OPENAI, "gpt-4o-mini", outputText, 50, 40, BigDecimal.ZERO, 100,
			LlmUsageStatus.SUCCESS, null, Map.of("purpose", "NEXT_WEEK_ADJUST"), "raw"
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
