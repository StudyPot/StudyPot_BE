package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.studypot.aistudyleader.curriculum.domain.CurriculumGeneration;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGenerationRequest;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStartContext;
import com.studypot.aistudyleader.curriculum.domain.LlmProvider;
import com.studypot.aistudyleader.curriculum.domain.SubmittedOnboardingResponse;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OpenAiCurriculumGeneratorTest {

	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000004321");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000004322");

	@Test
	void generateBuildsStructuredResponsesRequestAndParsesCurriculumJson() {
		CapturingTransport transport = new CapturingTransport("""
			{
			  "output": [
			    {
			      "type": "message",
			      "content": [
			        {
			          "type": "output_text",
			          "text": "{\\"title\\":\\"Spring Boot 6주 완성\\",\\"totalWeeks\\":1,\\"weeks\\":[{\\"weekNumber\\":1,\\"title\\":\\"JPA 기초\\",\\"sprintGoal\\":\\"핵심 개념을 맞춥니다.\\",\\"learningGoals\\":[\\"Entity 매핑 이해\\"],\\"resources\\":[{\\"title\\":\\"공식 문서\\",\\"url\\":\\"https://spring.io/projects/spring-boot\\"}],\\"tasks\\":[{\\"taskType\\":\\"READING\\",\\"title\\":\\"JPA 읽기\\",\\"description\\":\\"공식 문서를 읽습니다.\\",\\"required\\":true}]}]}"
			        }
			      ]
			    }
			  ],
			  "usage": {"input_tokens": 111, "output_tokens": 222}
			}
			""");
		OpenAiCurriculumGenerator generator = new OpenAiCurriculumGenerator(
			transport,
			new com.fasterxml.jackson.databind.ObjectMapper(),
			"gpt-4o-mini"
		);

		CurriculumGeneration result = generator.generate(request());

		assertThat(result.provider()).isEqualTo(LlmProvider.OPENAI);
		assertThat(result.model()).isEqualTo("gpt-4o-mini");
		assertThat(result.inputTokens()).isEqualTo(111);
		assertThat(result.outputTokens()).isEqualTo(222);
		assertThat(result.title()).isEqualTo("Spring Boot 6주 완성");
		assertThat(result.weeks()).hasSize(1);
		assertThat(result.weeks().getFirst().tasks().getFirst().taskType()).isEqualTo(WeeklyTaskType.READING);
		assertThat(transport.request).containsEntry("model", "gpt-4o-mini");
		assertThat(transport.request.get("text").toString()).contains("json_schema");
		assertThat(transport.request.get("input").toString()).contains("Backend Interview Study");
		assertThat(result.requestPayload()).containsEntry("purpose", "CURRICULUM_GENERATE");
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
				UUID.fromString("018f0000-0000-7000-8000-000000004323"),
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

	private static final class CapturingTransport implements OpenAiResponsesTransport {

		private final String response;
		private Map<String, Object> request;

		private CapturingTransport(String response) {
			this.response = response;
		}

		@Override
		public String createResponse(Map<String, Object> request) {
			this.request = request;
			return response;
		}
	}
}
