package com.studypot.aistudyleader.curriculum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;

class InvalidTaskCompletionRequestExceptionTest {

	@Test
	void exposesFieldAndMessage() {
		var exception = new InvalidTaskCompletionRequestException(
			"incompleteReason",
			"incomplete reason is required when status is INCOMPLETE."
		);

		assertThat(exception.field()).isEqualTo("incompleteReason");
		assertThat(exception.getMessage()).isEqualTo("incomplete reason is required when status is INCOMPLETE.");
	}

	@Test
	void preservesNullFieldAndMessageLikeOtherValidationExceptions() {
		var exception = new InvalidTaskCompletionRequestException(null, null);

		assertThat(exception.field()).isNull();
		assertThat(exception.getMessage()).isNull();
	}

	@Test
	void propagatesMessageWhenThrownAndCaught() {
		Throwable thrown = catchThrowable(() -> {
			throw new InvalidTaskCompletionRequestException("status", "invalid task status.");
		});

		assertThat(thrown)
			.isInstanceOf(InvalidTaskCompletionRequestException.class)
			.hasMessage("invalid task status.");
	}
}
