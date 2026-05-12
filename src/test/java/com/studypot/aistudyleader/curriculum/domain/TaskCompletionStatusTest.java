package com.studypot.aistudyleader.curriculum.domain;

import static com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus.DONE;
import static com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus.INCOMPLETE;
import static com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus.SKIPPED;
import static com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus.TODO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TaskCompletionStatusTest {

	@Test
	void valuesReturnStatusesInContractOrder() {
		assertThat(TaskCompletionStatus.values())
			.containsExactly(TODO, DONE, INCOMPLETE, SKIPPED);

		assertThat(TODO.name()).isEqualTo("TODO");
		assertThat(TODO.ordinal()).isZero();
		assertThat(DONE.name()).isEqualTo("DONE");
		assertThat(DONE.ordinal()).isEqualTo(1);
		assertThat(INCOMPLETE.name()).isEqualTo("INCOMPLETE");
		assertThat(INCOMPLETE.ordinal()).isEqualTo(2);
		assertThat(SKIPPED.name()).isEqualTo("SKIPPED");
		assertThat(SKIPPED.ordinal()).isEqualTo(3);
	}

	@Test
	void valueOfReturnsExpectedStatus() {
		assertThat(TaskCompletionStatus.valueOf("TODO")).isEqualTo(TODO);
		assertThat(TaskCompletionStatus.valueOf("DONE")).isEqualTo(DONE);
		assertThat(TaskCompletionStatus.valueOf("INCOMPLETE")).isEqualTo(INCOMPLETE);
		assertThat(TaskCompletionStatus.valueOf("SKIPPED")).isEqualTo(SKIPPED);
	}

	@Test
	void valueOfRejectsUnknownStatus() {
		assertThatThrownBy(() -> TaskCompletionStatus.valueOf("COMPLETE"))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
