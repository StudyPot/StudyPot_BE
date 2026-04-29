package com.studypot.aistudyleader.global.error;

import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	private final ProblemDetailFactory problemDetailFactory;

	public ApiExceptionHandler(ProblemDetailFactory problemDetailFactory) {
		this.problemDetailFactory = problemDetailFactory;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		var fieldErrors = exception.getBindingResult().getFieldErrors().stream()
			.map(fieldError -> new FieldErrorResponse(fieldError.getField(), messageOrDefault(fieldError.getDefaultMessage())))
			.sorted(Comparator.comparing(FieldErrorResponse::field))
			.toList();

		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
			.body(problemDetailFactory.validationProblem(fieldErrors));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception) {
		var fieldErrors = exception.getConstraintViolations().stream()
			.map(violation -> new FieldErrorResponse(violation.getPropertyPath().toString(), messageOrDefault(violation.getMessage())))
			.sorted(Comparator.comparing(FieldErrorResponse::field))
			.toList();

		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
			.body(problemDetailFactory.validationProblem(fieldErrors));
	}

	private static String messageOrDefault(String message) {
		return message == null || message.isBlank() ? "Invalid value." : message;
	}
}
