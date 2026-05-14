package com.studypot.aistudyleader.global.error;

import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailFactory {

	private static final URI VALIDATION_ERROR_TYPE = URI.create("https://api.studypot.com/problems/validation-error");
	private static final URI UNAUTHORIZED_TYPE = URI.create("https://api.studypot.com/problems/unauthorized");
	private static final URI FORBIDDEN_TYPE = URI.create("https://api.studypot.com/problems/forbidden");
	private static final URI NOT_FOUND_TYPE = URI.create("https://api.studypot.com/problems/not-found");
	private static final URI CONFLICT_TYPE = URI.create("https://api.studypot.com/problems/conflict");
	private static final URI TOO_MANY_REQUESTS_TYPE = URI.create("https://api.studypot.com/problems/too-many-requests");
	private static final URI SERVICE_UNAVAILABLE_TYPE = URI.create("https://api.studypot.com/problems/service-unavailable");

	public ProblemDetail validationProblem(List<FieldErrorResponse> fieldErrors) {
		ProblemDetail problemDetail = create(
			HttpStatus.UNPROCESSABLE_CONTENT,
			"Invalid request payload",
			"Request validation failed.",
			VALIDATION_ERROR_TYPE
		);
		problemDetail.setProperty("fieldErrors", List.copyOf(fieldErrors));
		return problemDetail;
	}

	public ProblemDetail unauthorized(String detail) {
		return create(HttpStatus.UNAUTHORIZED, "Unauthorized", detail, UNAUTHORIZED_TYPE);
	}

	public ProblemDetail forbidden(String detail) {
		return create(HttpStatus.FORBIDDEN, "Forbidden", detail, FORBIDDEN_TYPE);
	}

	public ProblemDetail notFound(String detail) {
		return create(HttpStatus.NOT_FOUND, "Not Found", detail, NOT_FOUND_TYPE);
	}

	public ProblemDetail conflict(String detail) {
		return create(HttpStatus.CONFLICT, "Conflict", detail, CONFLICT_TYPE);
	}

	public ProblemDetail tooManyRequests(String detail) {
		return create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", detail, TOO_MANY_REQUESTS_TYPE);
	}

	public ProblemDetail serviceUnavailable(String detail) {
		return create(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", detail, SERVICE_UNAVAILABLE_TYPE);
	}

	private static ProblemDetail create(HttpStatusCode status, String title, String detail, URI type) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
		problemDetail.setTitle(title);
		problemDetail.setType(type);
		return problemDetail;
	}
}
