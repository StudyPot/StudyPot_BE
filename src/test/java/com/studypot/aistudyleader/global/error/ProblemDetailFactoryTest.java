package com.studypot.aistudyleader.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProblemDetailFactoryTest {

	private final ProblemDetailFactory factory = new ProblemDetailFactory();

	@Test
	void validationProblemUsesUnprocessableEntityAndFieldErrors() {
		var fieldErrors = List.of(new FieldErrorResponse("name", "must not be blank"));

		var problemDetail = factory.validationProblem(fieldErrors);

		assertThat(problemDetail.getStatus()).isEqualTo(422);
		assertThat(problemDetail.getTitle()).isEqualTo("Invalid request payload");
		assertThat(problemDetail.getProperties()).containsEntry("fieldErrors", fieldErrors);
	}

	@Test
	void unauthorizedProblemUsesProblemDetailShape() {
		var problemDetail = factory.unauthorized("Authentication is required.");

		assertThat(problemDetail.getStatus()).isEqualTo(401);
		assertThat(problemDetail.getTitle()).isEqualTo("Unauthorized");
		assertThat(problemDetail.getDetail()).isEqualTo("Authentication is required.");
	}
}
