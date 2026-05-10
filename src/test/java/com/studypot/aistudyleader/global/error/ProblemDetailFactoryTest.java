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

	@Test
	void notFoundProblemUsesProblemDetailShape() {
		var problemDetail = factory.notFound("Study group was not found.");

		assertThat(problemDetail.getStatus()).isEqualTo(404);
		assertThat(problemDetail.getTitle()).isEqualTo("Not Found");
		assertThat(problemDetail.getDetail()).isEqualTo("Study group was not found.");
	}

	@Test
	void conflictProblemUsesProblemDetailShape() {
		var problemDetail = factory.conflict("Invite code does not match.");

		assertThat(problemDetail.getStatus()).isEqualTo(409);
		assertThat(problemDetail.getTitle()).isEqualTo("Conflict");
		assertThat(problemDetail.getDetail()).isEqualTo("Invite code does not match.");
	}

	@Test
	void serviceUnavailableProblemUsesProblemDetailShape() {
		var problemDetail = factory.serviceUnavailable("Auth service is unavailable.");

		assertThat(problemDetail.getStatus()).isEqualTo(503);
		assertThat(problemDetail.getTitle()).isEqualTo("Service Unavailable");
		assertThat(problemDetail.getDetail()).isEqualTo("Auth service is unavailable.");
	}
}
