package com.studypot.aistudyleader.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.studypot.aistudyleader.curriculum.service.InvalidTaskCompletionRequestException;
import com.studypot.aistudyleader.global.ratelimit.RateLimitDecision;
import com.studypot.aistudyleader.global.ratelimit.RateLimitExceededException;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.junit.jupiter.api.Test;

class ApiExceptionHandlerTaskCompletionTest {

	private final ApiExceptionHandler handler = new ApiExceptionHandler(new ProblemDetailFactory());

	@Test
	void invalidTaskCompletionRequestReturnsValidationProblem() {
		var response = handler.handleInvalidTaskCompletionRequest(new InvalidTaskCompletionRequestException(
			"incompleteReason",
			"incomplete reason is required when status is INCOMPLETE."
		));

		assertThat(response.getStatusCode().value()).isEqualTo(422);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getProperties()).containsEntry(
			"fieldErrors",
			List.of(new FieldErrorResponse(
				"incompleteReason",
				"incomplete reason is required when status is INCOMPLETE."
			))
		);
	}

	@Test
	void invalidTaskCompletionRequestUsesDefaultMessageWhenWhitespace() {
		var response = handler.handleInvalidTaskCompletionRequest(new InvalidTaskCompletionRequestException("status", " "));

		assertThat(response.getStatusCode().value()).isEqualTo(422);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getProperties()).containsEntry(
			"fieldErrors",
			List.of(new FieldErrorResponse("status", "Invalid value."))
		);
	}

	@Test
	void rateLimitExceededReturnsTooManyRequestsProblemWithRetryAfter() {
		var response = handler.handleRateLimitExceeded(new RateLimitExceededException(
			"current user lookup rate limit exceeded.",
			RateLimitDecision.rejected(61, 60, Duration.ofSeconds(13))
		));

		assertThat(response.getStatusCode().value()).isEqualTo(429);
		assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("13");
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getTitle()).isEqualTo("Too Many Requests");
		assertThat(response.getBody().getDetail()).isEqualTo("current user lookup rate limit exceeded.");
	}
}
