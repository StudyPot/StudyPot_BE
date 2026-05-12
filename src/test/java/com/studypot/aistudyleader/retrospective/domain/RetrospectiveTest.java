package com.studypot.aistudyleader.retrospective.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RetrospectiveTest {

	private static final UUID RETROSPECTIVE_ID = UUID.fromString("018f0000-0000-7000-8000-000000006001");
	private static final UUID PROGRESS_ID = UUID.fromString("018f0000-0000-7000-8000-000000006002");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000006003");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000006004");
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
	void triggerTypesMatchLockedJiraAndDbContract() {
		assertThat(Arrays.stream(RetrospectiveTriggerType.values()).map(Enum::name))
			.containsExactly("WEEK_ENDED", "INCOMPLETE_MODAL", "USER_CHAT", "MANUAL");
	}

	@Test
	void statusesMatchLockedDbAndOpenApiContract() {
		assertThat(Arrays.stream(RetrospectiveStatus.values()).map(Enum::name))
			.containsExactly("PENDING", "PROCESSING", "COMPLETED", "FAILED");
	}
}
