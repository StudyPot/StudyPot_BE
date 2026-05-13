package com.studypot.aistudyleader.curriculum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGeneration;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGenerationRequest;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStartContext;
import com.studypot.aistudyleader.curriculum.domain.SubmittedOnboardingResponse;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmStructuredRequest;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProviderBackedCurriculumGeneratorTest {

	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000004421");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000004422");

	@Test
	void generateUsesGenericProviderAndParsesValidatedCurriculumJson() {
		CapturingProvider provider = new CapturingProvider(new LlmStructuredResponse(
			LlmProvider.OPENAI,
			"gpt-4o-mini",
			"""
				{"title":"Spring Boot 6주 완성","totalWeeks":1,"weeks":[{"weekNumber":1,"title":"JPA 기초","sprintGoal":"핵심 개념을 맞춥니다.","learningGoals":["Entity 매핑 이해"],"resources":[{"title":"공식 문서","url":"https://spring.io/projects/spring-boot"}],"tasks":[{"taskType":"READING","title":"JPA 읽기","description":"공식 문서를 읽습니다.","required":true}]}]}
				""",
			111,
			222,
			BigDecimal.ZERO,
			90,
			LlmUsageStatus.SUCCESS,
			null,
			Map.of("purpose", "CURRICULUM_GENERATE"),
			"raw provider response"
		));
		ProviderBackedCurriculumGenerator generator = new ProviderBackedCurriculumGenerator(
			provider,
			JsonMapper.builder().findAndAddModules().build()
		);

		CurriculumGeneration result = generator.generate(request());

		assertThat(provider.request.purpose()).isEqualTo(LlmUsagePurpose.CURRICULUM_GENERATE);
		assertThat(provider.request.instructions()).contains("study curriculum");
		assertThat(provider.request.input()).containsKey("group");
		assertThat(provider.request.input().toString()).contains("Backend Interview Study");
		assertThat(provider.request.textFormat().toString()).contains("json_schema");
		assertThat(result.provider()).isEqualTo(LlmProvider.OPENAI);
		assertThat(result.model()).isEqualTo("gpt-4o-mini");
		assertThat(result.inputTokens()).isEqualTo(111);
		assertThat(result.outputTokens()).isEqualTo(222);
		assertThat(result.title()).isEqualTo("Spring Boot 6주 완성");
		assertThat(result.weeks()).hasSize(1);
		assertThat(result.weeks().getFirst().tasks().getFirst().taskType()).isEqualTo(WeeklyTaskType.READING);
		assertThat(result.requestPayload()).containsEntry("purpose", "CURRICULUM_GENERATE");
		assertThat(result.responseSummary()).isEqualTo("Generated curriculum title: Spring Boot 6주 완성, weeks: 1");
	}

	@Test
	void generateRejectsInvalidProviderOutputWithFailedUsageAudit() {
		CapturingProvider provider = new CapturingProvider(new LlmStructuredResponse(
			LlmProvider.OPENAI,
			"gpt-4o-mini",
			"""
				{"title":"Spring Boot 6주 완성","totalWeeks":1,"weeks":[{"weekNumber":1,"title":"JPA 기초","sprintGoal":"핵심 개념을 맞춥니다.","learningGoals":["Entity 매핑 이해"],"resources":[],"tasks":[{"taskType":"UNKNOWN","title":"JPA 읽기","description":"공식 문서를 읽습니다.","required":true}]}]}
				""",
			111,
			222,
			BigDecimal.ZERO,
			90,
			LlmUsageStatus.SUCCESS,
			null,
			Map.of("purpose", "CURRICULUM_GENERATE"),
			"raw provider response"
		));
		ProviderBackedCurriculumGenerator generator = new ProviderBackedCurriculumGenerator(
			provider,
			JsonMapper.builder().findAndAddModules().build()
		);

		assertThatThrownBy(() -> generator.generate(request()))
			.isInstanceOf(CurriculumGenerationException.class)
			.hasMessage("curriculum generation output was invalid.")
			.satisfies(exception -> {
				CurriculumGenerationException generationException = (CurriculumGenerationException) exception;
				assertThat(generationException.failure()).hasValueSatisfying(failure -> {
					assertThat(failure.purpose()).isEqualTo(LlmUsagePurpose.CURRICULUM_GENERATE);
					assertThat(failure.provider()).isEqualTo(LlmProvider.OPENAI);
					assertThat(failure.status()).isEqualTo(LlmUsageStatus.FAILED);
					assertThat(failure.errorCode()).isEqualTo("CURRICULUM_RESPONSE_INVALID");
				});
			});
	}

	private static CurriculumGenerationRequest request() {
		return new CurriculumGenerationRequest(
			new CurriculumStartContext(
				GROUP_ID,
				"Backend Interview Study",
				"Spring Boot",
				List.of("JPA", "Security"),
				StudyGroupStatus.ONBOARDING,
				LocalDate.parse("2026-05-11"),
				LocalDate.parse("2026-06-21"),
				MEMBER_ID,
				GroupMemberPermission.OWNER,
				GroupMemberStatus.PENDING_ONBOARDING
			),
			List.of(new SubmittedOnboardingResponse(
				UUID.fromString("018f0000-0000-7000-8000-000000004423"),
				MEMBER_ID,
				Map.of("JPA", 2),
				Map.of("READING", 4),
				"실습 위주",
				List.of(),
				Instant.parse("2026-05-10T08:00:00Z")
			)),
			Map.of("submittedResponseCount", 1),
			Instant.parse("2026-05-11T03:30:00Z")
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
