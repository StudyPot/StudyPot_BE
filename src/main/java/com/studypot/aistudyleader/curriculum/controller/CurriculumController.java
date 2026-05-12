package com.studypot.aistudyleader.curriculum.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.curriculum.domain.Curriculum;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStatus;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeek;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeekStatus;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgress;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgressStatus;
import com.studypot.aistudyleader.curriculum.domain.TaskCompletion;
import com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTask;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import com.studypot.aistudyleader.curriculum.service.CurriculumService;
import com.studypot.aistudyleader.curriculum.service.CurriculumServiceUnavailableException;
import com.studypot.aistudyleader.curriculum.service.CompleteTaskCommand;
import com.studypot.aistudyleader.curriculum.service.GetCurrentWeekQuery;
import com.studypot.aistudyleader.curriculum.service.GetCurriculumQuery;
import com.studypot.aistudyleader.curriculum.service.ListWeeklyTasksQuery;
import com.studypot.aistudyleader.curriculum.service.StartCurriculumCommand;
import com.studypot.aistudyleader.curriculum.service.UpdateWeekProgressCommand;
import com.studypot.aistudyleader.global.api.ApiPaths;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class CurriculumController {

	private final ObjectProvider<CurriculumService> curriculumService;

	@PostMapping(ApiPaths.V1 + "/groups/{groupId}/start")
	@ResponseStatus(HttpStatus.CREATED)
	CurriculumResponse startStudy(Authentication authentication, @PathVariable UUID groupId) {
		Curriculum curriculum = service().startStudy(new StartCurriculumCommand(authenticatedUserId(authentication), groupId));
		return CurriculumResponse.from(curriculum);
	}

	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/curriculum")
	CurriculumResponse getCurriculum(Authentication authentication, @PathVariable UUID groupId) {
		Curriculum curriculum = service().getCurriculum(new GetCurriculumQuery(authenticatedUserId(authentication), groupId));
		return CurriculumResponse.from(curriculum);
	}

	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/weeks/current")
	CurriculumWeekResponse getCurrentWeek(Authentication authentication, @PathVariable UUID groupId) {
		CurriculumWeek week = service().getCurrentWeek(new GetCurrentWeekQuery(authenticatedUserId(authentication), groupId));
		return CurriculumWeekResponse.from(week);
	}

	@GetMapping(ApiPaths.V1 + "/weeks/{weekId}/tasks")
	List<WeeklyTaskResponse> listWeeklyTasks(Authentication authentication, @PathVariable UUID weekId) {
		return service().listWeeklyTasks(new ListWeeklyTasksQuery(authenticatedUserId(authentication), weekId)).stream()
			.map(WeeklyTaskResponse::from)
			.toList();
	}

	@PutMapping(ApiPaths.V1 + "/weeks/{weekId}/progress/me")
	MemberWeekProgressResponse updateMyWeekProgress(
		Authentication authentication,
		@PathVariable UUID weekId,
		@Valid @RequestBody UpdateWeekProgressRequest request
	) {
		MemberWeekProgress progress = service().updateMyWeekProgress(request.toCommand(authenticatedUserId(authentication), weekId));
		return MemberWeekProgressResponse.from(progress);
	}

	@PostMapping(ApiPaths.V1 + "/tasks/{taskId}/completion/me")
	TaskCompletionResponse completeTask(
		Authentication authentication,
		@PathVariable UUID taskId,
		@Valid @RequestBody TaskCompletionRequest request
	) {
		TaskCompletion completion = service().completeMyTask(request.toCommand(authenticatedUserId(authentication), taskId));
		return TaskCompletionResponse.from(completion);
	}

	private CurriculumService service() {
		return curriculumService.getIfAvailable(() -> {
			throw new CurriculumServiceUnavailableException("curriculum service is not configured.");
		});
	}

	private static UUID authenticatedUserId(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AuthSessionRejectedException("authenticated user is required.");
		}
		String subject = authenticatedSubject(authentication);
		if (subject == null || subject.isBlank()) {
			throw new AuthSessionRejectedException("authenticated user is required.");
		}
		try {
			return UUID.fromString(subject);
		} catch (IllegalArgumentException exception) {
			throw new AuthSessionRejectedException("authenticated user is invalid.");
		}
	}

	private static String authenticatedSubject(Authentication authentication) {
		Object principal = authentication.getPrincipal();
		if (principal instanceof Jwt jwt) {
			return jwt.getSubject();
		}
		return authentication.getName();
	}

	private record CurriculumResponse(
		UUID id,
		UUID groupId,
		String title,
		int totalWeeks,
		Map<String, Object> onboardingSummary,
		CurriculumStatus status
	) {

		private static CurriculumResponse from(Curriculum curriculum) {
			return new CurriculumResponse(
				curriculum.id(),
				curriculum.groupId(),
				curriculum.title(),
				curriculum.totalWeeks(),
				curriculum.onboardingSummary(),
				curriculum.status()
			);
		}
	}

	private record CurriculumWeekResponse(
		UUID id,
		UUID curriculumId,
		int weekNumber,
		String title,
		String sprintGoal,
		CurriculumWeekStatus status,
		Instant startsAt,
		Instant endsAt
	) {

		private static CurriculumWeekResponse from(CurriculumWeek week) {
			return new CurriculumWeekResponse(
				week.id(),
				week.curriculumId(),
				week.weekNumber(),
				week.title(),
				week.sprintGoal(),
				week.status(),
				week.startsAt(),
				week.endsAt()
			);
		}
	}

	private record WeeklyTaskResponse(
		UUID id,
		UUID curriculumWeekId,
		int displayOrder,
		WeeklyTaskType taskType,
		String title,
		String description,
		boolean required,
		Instant dueAt
	) {

		private static WeeklyTaskResponse from(WeeklyTask task) {
			return new WeeklyTaskResponse(
				task.id(),
				task.curriculumWeekId(),
				task.displayOrder(),
				task.taskType(),
				task.title(),
				task.description(),
				task.required(),
				task.dueAt()
			);
		}
	}

	private record UpdateWeekProgressRequest(
		@NotNull
		MemberWeekProgressStatus status,
		String completionNote,
		String incompleteReason
	) {

		UpdateWeekProgressCommand toCommand(UUID authenticatedUserId, UUID weekId) {
			return new UpdateWeekProgressCommand(authenticatedUserId, weekId, status, completionNote, incompleteReason);
		}
	}

	// completionNote is intentionally omitted to match the locked OpenAPI MemberWeekProgressResponse.
	private record MemberWeekProgressResponse(
		UUID id,
		MemberWeekProgressStatus status,
		Instant completedAt,
		String incompleteReason
	) {

		private static MemberWeekProgressResponse from(MemberWeekProgress progress) {
			return new MemberWeekProgressResponse(
				progress.id(),
				progress.status(),
				progress.completedAt(),
				progress.incompleteReason()
			);
		}
	}

	private record TaskCompletionRequest(
		@NotNull
		TaskCompletionStatus status,
		String completionNote,
		String incompleteReason,
		String evidenceUrl
	) {

		CompleteTaskCommand toCommand(UUID authenticatedUserId, UUID taskId) {
			return new CompleteTaskCommand(authenticatedUserId, taskId, status, completionNote, incompleteReason, evidenceUrl);
		}
	}

	// completionNote and evidenceUrl are stored but intentionally omitted to match locked OpenAPI.
	private record TaskCompletionResponse(
		UUID id,
		TaskCompletionStatus status,
		Instant completedAt,
		String incompleteReason
	) {

		private static TaskCompletionResponse from(TaskCompletion completion) {
			return new TaskCompletionResponse(
				completion.id(),
				completion.status(),
				completion.completedAt(),
				completion.incompleteReason()
			);
		}
	}
}
