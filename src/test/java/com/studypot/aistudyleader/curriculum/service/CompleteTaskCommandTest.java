package com.studypot.aistudyleader.curriculum.service;

import static com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus.DONE;
import static com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus.SKIPPED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompleteTaskCommandTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000003701");
	private static final UUID TASK_ID = UUID.fromString("018f0000-0000-7000-8000-000000003702");

	@Test
	void exposesCommandComponents() {
		var command = new CompleteTaskCommand(
			USER_ID,
			TASK_ID,
			DONE,
			"정리 완료",
			null,
			"https://example.com/evidence"
		);

		assertThat(command.authenticatedUserId()).isEqualTo(USER_ID);
		assertThat(command.taskId()).isEqualTo(TASK_ID);
		assertThat(command.status()).isEqualTo(DONE);
		assertThat(command.completionNote()).isEqualTo("정리 완료");
		assertThat(command.incompleteReason()).isNull();
		assertThat(command.evidenceUrl()).isEqualTo("https://example.com/evidence");
	}

	@Test
	void allowsOptionalTextFieldsToBeNullOrEmpty() {
		var command = new CompleteTaskCommand(USER_ID, TASK_ID, SKIPPED, "", null, "");

		assertThat(command.completionNote()).isEmpty();
		assertThat(command.incompleteReason()).isNull();
		assertThat(command.evidenceUrl()).isEmpty();
	}

	@Test
	void rejectsNullAuthenticatedUserId() {
		assertThatThrownBy(() -> new CompleteTaskCommand(null, TASK_ID, DONE, null, null, null))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("authenticatedUserId must not be null");
	}

	@Test
	void rejectsNullTaskId() {
		assertThatThrownBy(() -> new CompleteTaskCommand(USER_ID, null, DONE, null, null, null))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("taskId must not be null");
	}

	@Test
	void rejectsNullStatus() {
		assertThatThrownBy(() -> new CompleteTaskCommand(USER_ID, TASK_ID, null, null, null, null))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("status must not be null");
	}
}
