package com.studypot.aistudyleader.curriculum.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TaskCompletionUpdateRejectedExceptionTest {

	@Test
	void exposesMessageAndRuntimeExceptionType() {
		var exception = new TaskCompletionUpdateRejectedException("task completion could not be updated.");

		assertThat(exception).isInstanceOf(RuntimeException.class);
		assertThat(exception.getMessage()).isEqualTo("task completion could not be updated.");
	}

	@Test
	void preservesEmptyMessage() {
		var exception = new TaskCompletionUpdateRejectedException("");

		assertThat(exception.getMessage()).isEmpty();
	}

	@Test
	void preservesNullMessage() {
		var exception = new TaskCompletionUpdateRejectedException(null);

		assertThat(exception.getMessage()).isNull();
	}
}
