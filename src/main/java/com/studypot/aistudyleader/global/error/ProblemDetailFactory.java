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

	public ProblemDetail validationProblem(List<FieldErrorResponse> fieldErrors) {
		ProblemDetail problemDetail = create(
			HttpStatus.UNPROCESSABLE_ENTITY,
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

	private static ProblemDetail create(HttpStatusCode status, String title, String detail, URI type) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
		problemDetail.setTitle(title);
		problemDetail.setType(type);
		return problemDetail;
	}
}
