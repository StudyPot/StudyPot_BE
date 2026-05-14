package com.studypot.aistudyleader.global.error;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.auth.service.AuthServiceUnavailableException;
import com.studypot.aistudyleader.auth.service.InvalidAuthRequestException;
import com.studypot.aistudyleader.auth.service.OAuthLoginRejectedException;
import com.studypot.aistudyleader.ai.repository.AiConversationPersistenceException;
import com.studypot.aistudyleader.ai.service.AiConversationAccessDeniedException;
import com.studypot.aistudyleader.ai.service.AiConversationMutationRejectedException;
import com.studypot.aistudyleader.ai.service.AiConversationNotFoundException;
import com.studypot.aistudyleader.ai.service.AiConversationResponseGenerationException;
import com.studypot.aistudyleader.ai.service.AiConversationServiceUnavailableException;
import com.studypot.aistudyleader.ai.service.InvalidAiConversationRequestException;
import com.studypot.aistudyleader.curriculum.service.CurriculumAccessDeniedException;
import com.studypot.aistudyleader.curriculum.service.CurriculumGenerationException;
import com.studypot.aistudyleader.curriculum.service.CurriculumGroupNotFoundException;
import com.studypot.aistudyleader.curriculum.service.CurriculumNotFoundException;
import com.studypot.aistudyleader.curriculum.service.CurriculumServiceUnavailableException;
import com.studypot.aistudyleader.curriculum.service.CurriculumStartRejectedException;
import com.studypot.aistudyleader.curriculum.service.InvalidTaskCompletionRequestException;
import com.studypot.aistudyleader.curriculum.service.InvalidWeekProgressRequestException;
import com.studypot.aistudyleader.curriculum.service.TaskCompletionUpdateRejectedException;
import com.studypot.aistudyleader.curriculum.service.WeekProgressUpdateRejectedException;
import com.studypot.aistudyleader.llm.repository.LlmUsagePersistenceException;
import com.studypot.aistudyleader.llm.service.LlmUsageAccessDeniedException;
import com.studypot.aistudyleader.llm.service.LlmUsageGroupNotFoundException;
import com.studypot.aistudyleader.llm.service.LlmUsageServiceUnavailableException;
import com.studypot.aistudyleader.notification.repository.NotificationPersistenceException;
import com.studypot.aistudyleader.notification.service.NotificationAccessDeniedException;
import com.studypot.aistudyleader.notification.service.NotificationGroupNotFoundException;
import com.studypot.aistudyleader.notification.service.NotificationMutationRejectedException;
import com.studypot.aistudyleader.notification.service.NotificationNotFoundException;
import com.studypot.aistudyleader.notification.service.NotificationServiceUnavailableException;
import com.studypot.aistudyleader.onboarding.service.InvalidOnboardingRequestException;
import com.studypot.aistudyleader.onboarding.service.OnboardingGroupNotFoundException;
import com.studypot.aistudyleader.onboarding.service.OnboardingMembershipRequiredException;
import com.studypot.aistudyleader.onboarding.service.OnboardingResponseNotFoundException;
import com.studypot.aistudyleader.onboarding.service.OnboardingServiceUnavailableException;
import com.studypot.aistudyleader.global.ratelimit.RateLimitExceededException;
import com.studypot.aistudyleader.retrospective.repository.RetrospectivePersistenceException;
import com.studypot.aistudyleader.retrospective.service.RetrospectiveAccessDeniedException;
import com.studypot.aistudyleader.retrospective.service.RetrospectiveMutationRejectedException;
import com.studypot.aistudyleader.retrospective.service.RetrospectiveNotFoundException;
import com.studypot.aistudyleader.retrospective.service.RetrospectiveServiceUnavailableException;
import com.studypot.aistudyleader.studygroup.service.StudyGroupJoinRejectedException;
import com.studypot.aistudyleader.studygroup.service.StudyGroupNotFoundException;
import com.studypot.aistudyleader.studygroup.service.StudyGroupServiceUnavailableException;
import com.studypot.aistudyleader.studygroup.rules.repository.GroupRulePersistenceException;
import com.studypot.aistudyleader.studygroup.rules.service.GroupRuleAccessDeniedException;
import com.studypot.aistudyleader.studygroup.rules.service.GroupRuleGroupNotFoundException;
import com.studypot.aistudyleader.studygroup.rules.service.GroupRuleMutationRejectedException;
import com.studypot.aistudyleader.studygroup.rules.service.GroupRuleNotFoundException;
import com.studypot.aistudyleader.studygroup.rules.service.GroupRuleServiceUnavailableException;
import com.studypot.aistudyleader.studygroup.rules.service.InvalidGroupRuleRequestException;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

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

		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
			.body(problemDetailFactory.validationProblem(fieldErrors));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception) {
		var fieldErrors = exception.getConstraintViolations().stream()
			.map(violation -> new FieldErrorResponse(violation.getPropertyPath().toString(), messageOrDefault(violation.getMessage())))
			.sorted(Comparator.comparing(FieldErrorResponse::field))
			.toList();

		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
			.body(problemDetailFactory.validationProblem(fieldErrors));
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ProblemDetail> handleHandlerMethodValidation(HandlerMethodValidationException exception) {
		var fieldErrors = exception.getParameterValidationResults().stream()
			.flatMap(result -> result.getResolvableErrors().stream()
				.map(error -> new FieldErrorResponse(parameterName(result), messageOrDefault(error.getDefaultMessage()))))
			.sorted(Comparator.comparing(FieldErrorResponse::field))
			.toList();

		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
			.body(problemDetailFactory.validationProblem(fieldErrors));
	}

	@ExceptionHandler(InvalidAuthRequestException.class)
	public ResponseEntity<ProblemDetail> handleInvalidAuthRequest(InvalidAuthRequestException exception) {
		var fieldErrors = List.of(new FieldErrorResponse(exception.field(), messageOrDefault(exception.getMessage())));
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
			.body(problemDetailFactory.validationProblem(fieldErrors));
	}

	@ExceptionHandler({AuthSessionRejectedException.class, OAuthLoginRejectedException.class})
	public ResponseEntity<ProblemDetail> handleAuthSessionRejected(RuntimeException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(problemDetailFactory.unauthorized(messageOrDefault(exception.getMessage())));
	}

	@ExceptionHandler(AuthServiceUnavailableException.class)
	public ResponseEntity<ProblemDetail> handleAuthServiceUnavailable(AuthServiceUnavailableException exception) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
			.body(problemDetailFactory.serviceUnavailable(messageOrDefault(exception.getMessage())));
	}

	@ExceptionHandler(StudyGroupServiceUnavailableException.class)
	public ResponseEntity<ProblemDetail> handleStudyGroupServiceUnavailable(StudyGroupServiceUnavailableException exception) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
			.body(problemDetailFactory.serviceUnavailable(messageOrDefault(exception.getMessage())));
	}

	@ExceptionHandler(OnboardingServiceUnavailableException.class)
	public ResponseEntity<ProblemDetail> handleOnboardingServiceUnavailable(OnboardingServiceUnavailableException exception) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
			.body(problemDetailFactory.serviceUnavailable(messageOrDefault(exception.getMessage())));
	}

	@ExceptionHandler({GroupRuleServiceUnavailableException.class, GroupRulePersistenceException.class})
	public ResponseEntity<ProblemDetail> handleGroupRuleServiceUnavailable(RuntimeException exception) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
			.body(problemDetailFactory.serviceUnavailable(messageOrDefault(exception.getMessage())));
	}

	@ExceptionHandler({CurriculumServiceUnavailableException.class, CurriculumGenerationException.class})
	public ResponseEntity<ProblemDetail> handleCurriculumServiceUnavailable(RuntimeException exception) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
			.body(problemDetailFactory.serviceUnavailable(messageOrDefault(exception.getMessage())));
	}

	@ExceptionHandler({
		RetrospectiveServiceUnavailableException.class,
		RetrospectivePersistenceException.class,
		AiConversationServiceUnavailableException.class,
		AiConversationResponseGenerationException.class,
		AiConversationPersistenceException.class,
		LlmUsageServiceUnavailableException.class,
		LlmUsagePersistenceException.class,
		NotificationServiceUnavailableException.class,
		NotificationPersistenceException.class
	})
	public ResponseEntity<ProblemDetail> handleRetrospectiveAndAiServiceUnavailable(RuntimeException exception) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
			.body(problemDetailFactory.serviceUnavailable(messageOrDefault(exception.getMessage())));
	}

	@ExceptionHandler({
		StudyGroupNotFoundException.class,
		OnboardingGroupNotFoundException.class,
		OnboardingResponseNotFoundException.class,
		CurriculumGroupNotFoundException.class,
		CurriculumNotFoundException.class,
		GroupRuleGroupNotFoundException.class,
		GroupRuleNotFoundException.class,
		RetrospectiveNotFoundException.class,
		AiConversationNotFoundException.class,
		LlmUsageGroupNotFoundException.class,
		NotificationGroupNotFoundException.class,
		NotificationNotFoundException.class
	})
	public ResponseEntity<ProblemDetail> handleResourceNotFound(RuntimeException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(problemDetailFactory.notFound(messageOrDefault(exception.getMessage())));
	}

	@ExceptionHandler({
		OnboardingMembershipRequiredException.class,
		CurriculumAccessDeniedException.class,
		GroupRuleAccessDeniedException.class,
		RetrospectiveAccessDeniedException.class,
		AiConversationAccessDeniedException.class,
		LlmUsageAccessDeniedException.class,
		NotificationAccessDeniedException.class
	})
	public ResponseEntity<ProblemDetail> handleForbidden(RuntimeException exception) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
			.body(problemDetailFactory.forbidden(messageOrDefault(exception.getMessage())));
	}

	@ExceptionHandler({
		StudyGroupJoinRejectedException.class,
		CurriculumStartRejectedException.class,
		TaskCompletionUpdateRejectedException.class,
		WeekProgressUpdateRejectedException.class,
		GroupRuleMutationRejectedException.class,
		RetrospectiveMutationRejectedException.class,
		AiConversationMutationRejectedException.class,
		NotificationMutationRejectedException.class
	})
	public ResponseEntity<ProblemDetail> handleConflict(RuntimeException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(problemDetailFactory.conflict(messageOrDefault(exception.getMessage())));
	}

	@ExceptionHandler(RateLimitExceededException.class)
	public ResponseEntity<ProblemDetail> handleRateLimitExceeded(RateLimitExceededException exception) {
		long retryAfterSeconds = Math.max(0, exception.decision().retryAfter().toSeconds());
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
			.header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds))
			.body(problemDetailFactory.tooManyRequests(messageOrDefault(exception.getMessage())));
	}

	@ExceptionHandler(InvalidOnboardingRequestException.class)
	public ResponseEntity<ProblemDetail> handleInvalidOnboardingRequest(InvalidOnboardingRequestException exception) {
		var fieldErrors = List.of(new FieldErrorResponse(exception.field(), messageOrDefault(exception.getMessage())));
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
			.body(problemDetailFactory.validationProblem(fieldErrors));
	}

	@ExceptionHandler(InvalidGroupRuleRequestException.class)
	public ResponseEntity<ProblemDetail> handleInvalidGroupRuleRequest(InvalidGroupRuleRequestException exception) {
		var fieldErrors = List.of(new FieldErrorResponse(exception.field(), messageOrDefault(exception.getMessage())));
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
			.body(problemDetailFactory.validationProblem(fieldErrors));
	}

	@ExceptionHandler(InvalidWeekProgressRequestException.class)
	public ResponseEntity<ProblemDetail> handleInvalidWeekProgressRequest(InvalidWeekProgressRequestException exception) {
		var fieldErrors = List.of(new FieldErrorResponse(exception.field(), messageOrDefault(exception.getMessage())));
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
			.body(problemDetailFactory.validationProblem(fieldErrors));
	}

	@ExceptionHandler(InvalidTaskCompletionRequestException.class)
	public ResponseEntity<ProblemDetail> handleInvalidTaskCompletionRequest(InvalidTaskCompletionRequestException exception) {
		var fieldErrors = List.of(new FieldErrorResponse(exception.field(), messageOrDefault(exception.getMessage())));
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
			.body(problemDetailFactory.validationProblem(fieldErrors));
	}

	@ExceptionHandler(InvalidAiConversationRequestException.class)
	public ResponseEntity<ProblemDetail> handleInvalidAiConversationRequest(InvalidAiConversationRequestException exception) {
		var fieldErrors = List.of(new FieldErrorResponse(exception.field(), messageOrDefault(exception.getMessage())));
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
			.body(problemDetailFactory.validationProblem(fieldErrors));
	}

	private static String parameterName(ParameterValidationResult result) {
		MethodParameter methodParameter = result.getMethodParameter();
		RequestParam requestParam = methodParameter.getParameterAnnotation(RequestParam.class);
		if (requestParam != null) {
			return annotationName(requestParam.name(), requestParam.value(), methodParameter);
		}

		String parameterName = methodParameter.getParameterName();
		return parameterName == null || parameterName.isBlank() ? methodParameter.getParameter().getName() : parameterName;
	}

	private static String annotationName(String name, String value, MethodParameter methodParameter) {
		if (!name.isBlank()) {
			return name;
		}
		if (!value.isBlank()) {
			return value;
		}

		String parameterName = methodParameter.getParameterName();
		return parameterName == null || parameterName.isBlank() ? methodParameter.getParameter().getName() : parameterName;
	}

	private static String messageOrDefault(String message) {
		return message == null || message.isBlank() ? "Invalid value." : message;
	}
}
