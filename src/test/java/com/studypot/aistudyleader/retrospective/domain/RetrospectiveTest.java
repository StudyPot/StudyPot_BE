package com.studypot.aistudyleader.retrospective.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RetrospectiveTest {

	private static final UUID RETROSPECTIVE_ID = UUID.fromString("018f0000-0000-7000-8000-000000006001");
	private static final UUID PROGRESS_ID = UUID.fromString("018f0000-0000-7000-8000-000000006002");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000006003");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000006004");
	private static final UUID LLM_USAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000006005");
	private static final Instant NOW = Instant.parse("2026-05-12T02:30:00Z");

	@Test
	void requestedRetrospectiveStartsPendingWithoutAiOutput() {
		Retrospective retrospective = Retrospective.requested(
			RETROSPECTIVE_ID,
			PROGRESS_ID,
			WEEK_ID,
			MEMBER_ID,
			RetrospectiveTriggerType.MANUAL,
			Map.of("progress", Map.of("status", "COMPLETED")),
			NOW
		);

		assertThat(retrospective.id()).isEqualTo(RETROSPECTIVE_ID);
		assertThat(retrospective.progressId()).isEqualTo(PROGRESS_ID);
		assertThat(retrospective.curriculumWeekId()).isEqualTo(WEEK_ID);
		assertThat(retrospective.memberId()).isEqualTo(MEMBER_ID);
		assertThat(retrospective.triggerType()).isEqualTo(RetrospectiveTriggerType.MANUAL);
		assertThat(retrospective.status()).isEqualTo(RetrospectiveStatus.PENDING);
		assertThat(retrospective.requestedAt()).isEqualTo(NOW);
		assertThat(retrospective.completedAt()).isNull();
		assertThat(retrospective.aiFeedback()).isEmpty();
		assertThat(retrospective.nextWeekAdjustment()).isEmpty();
		assertThat(retrospective.inputSummary()).containsKey("progress");
	}

	@Test
	void requestedRejectsMissingInputSummary() {
		assertThatThrownBy(() -> Retrospective.requested(
				RETROSPECTIVE_ID,
				PROGRESS_ID,
				WEEK_ID,
				MEMBER_ID,
				RetrospectiveTriggerType.MANUAL,
				Map.of(),
				NOW
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("inputSummary must not be empty.");
		assertThatThrownBy(() -> Retrospective.requested(
				RETROSPECTIVE_ID,
				PROGRESS_ID,
				WEEK_ID,
				MEMBER_ID,
				RetrospectiveTriggerType.MANUAL,
				null,
				NOW
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("inputSummary must not be empty.");
	}

	@Test
	void requestedRejectsNullTopLevelInputSummaryEntries() {
		Map<String, Object> inputSummary = new LinkedHashMap<>();
		inputSummary.put("progress", null);

		assertThatThrownBy(() -> Retrospective.requested(
				RETROSPECTIVE_ID,
				PROGRESS_ID,
				WEEK_ID,
				MEMBER_ID,
				RetrospectiveTriggerType.MANUAL,
				inputSummary,
				NOW
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("inputSummary must not contain null keys or values.");
	}

	@Test
	void completeWithFeedbackMapsOutputAndTimestamps() {
		Retrospective retrospective = requestedRetrospective();
		RetrospectiveFeedbackResult feedbackResult = RetrospectiveFeedbackResult.of(
			"실습 시간이 부족해 과제 일부가 미완료되었습니다.",
			java.util.List.of("개념 정리는 완료했습니다."),
			java.util.List.of("실습 과제 난이도가 높았습니다."),
			java.util.List.of("다음 주 실습을 두 단계로 나눕니다."),
			Map.of("difficulty", "slightly_lower", "taskChanges", java.util.List.of("필수 실습 1개 분리"))
		);

		Retrospective completed = retrospective.completeWithFeedback(LLM_USAGE_ID, feedbackResult, NOW.plusSeconds(120));

		assertThat(completed.status()).isEqualTo(RetrospectiveStatus.COMPLETED);
		assertThat(completed.llmUsageId()).isEqualTo(LLM_USAGE_ID);
		assertThat(completed.completedAt()).isEqualTo(NOW.plusSeconds(120));
		assertThat(completed.updatedAt()).isEqualTo(NOW.plusSeconds(120));
		assertThat(completed.aiFeedback()).containsEntry("summary", "실습 시간이 부족해 과제 일부가 미완료되었습니다.");
		assertThat(completed.nextWeekAdjustment()).containsEntry("difficulty", "slightly_lower");
		assertThat(completed.requestedAt()).isEqualTo(retrospective.requestedAt());
	}

	@Test
	void completeWithFeedbackRequiresLlmUsageId() {
		Retrospective retrospective = requestedRetrospective();
		RetrospectiveFeedbackResult feedbackResult = RetrospectiveFeedbackResult.of(
			"요약",
			java.util.List.of(),
			java.util.List.of(),
			java.util.List.of(),
			Map.of()
		);

		assertThatThrownBy(() -> retrospective.completeWithFeedback(null, feedbackResult, NOW.plusSeconds(120)))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("llmUsageId must not be null");
	}

	@Test
	void failWithFeedbackErrorRecordsSafeFailureSummary() {
		Retrospective retrospective = requestedRetrospective();

		Retrospective failed = retrospective.failFeedback(LLM_USAGE_ID, "PROVIDER_TIMEOUT", "LLM provider timed out", NOW.plusSeconds(60));

		assertThat(failed.status()).isEqualTo(RetrospectiveStatus.FAILED);
		assertThat(failed.llmUsageId()).isEqualTo(LLM_USAGE_ID);
		assertThat(failed.completedAt()).isNull();
		assertThat(failed.updatedAt()).isEqualTo(NOW.plusSeconds(60));
		assertThat(failed.nextWeekAdjustment()).isEmpty();
		assertThat(failed.aiFeedback()).containsKey("error");
		@SuppressWarnings("unchecked")
		Map<String, Object> error = (Map<String, Object>) failed.aiFeedback().get("error");
		assertThat(error)
			.containsEntry("code", "PROVIDER_TIMEOUT")
			.containsEntry("message", "LLM provider timed out");
	}

	@Test
	void failFeedbackRequiresLlmUsageId() {
		Retrospective retrospective = requestedRetrospective();

		assertThatThrownBy(() -> retrospective.failFeedback(null, "PROVIDER_TIMEOUT", "LLM provider timed out", NOW.plusSeconds(60)))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("llmUsageId must not be null");
	}

	@Test
	void triggerTypesMatchLockedJiraAndDbContract() {
		assertThat(Arrays.stream(RetrospectiveTriggerType.values()).map(Enum::name))
			.containsExactly("WEEK_ENDED", "INCOMPLETE_MODAL", "USER_CHAT", "MANUAL");
	}

	@Test
	void statusesMatchLockedDbAndOpenApiContract() {
		assertThat(Arrays.stream(RetrospectiveStatus.values()).map(Enum::name))
			.containsExactly("PENDING", "PROCESSING", "COMPLETED", "FAILED");
	}

	private static Retrospective requestedRetrospective() {
		return Retrospective.requested(
			RETROSPECTIVE_ID,
			PROGRESS_ID,
			WEEK_ID,
			MEMBER_ID,
			RetrospectiveTriggerType.MANUAL,
			Map.of("progress", Map.of("status", "COMPLETED")),
			NOW
		);
	}
}
