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
import com.studypot.aistudyleader.curriculum.service.GetWeekProgressQuery;
import com.studypot.aistudyleader.curriculum.service.ListWeeklyTasksQuery;
import com.studypot.aistudyleader.curriculum.service.StartCurriculumCommand;
import com.studypot.aistudyleader.curriculum.service.UpdateWeekProgressCommand;
import com.studypot.aistudyleader.global.api.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "커리큘럼/주차 Todo", description = "그룹 시작 후 커리큘럼, 현재 주차, 주차 과제, 멤버 진행 상태와 과제 완료를 다루는 API입니다.")
@RestController
@RequiredArgsConstructor
class CurriculumController {

	private final ObjectProvider<CurriculumService> curriculumService;

	@Operation(
		summary = "스터디 시작 및 커리큘럼 생성",
		description = "그룹장의 시작 요청으로 제출된 온보딩 응답을 요약해 커리큘럼을 생성하고 그룹을 진행 상태로 전환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "커리큘럼이 생성되고 그룹 시작 처리 완료"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "그룹장이 아니어서 시작할 수 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "409", description = "이미 시작되었거나 시작 조건을 만족하지 못함"),
		@ApiResponse(responseCode = "503", description = "커리큘럼 서비스가 아직 구성되지 않음")
	})
	@PostMapping(ApiPaths.V1 + "/groups/{groupId}/start")
	@ResponseStatus(HttpStatus.CREATED)
	CurriculumResponse startStudy(
		Authentication authentication,
		@Parameter(description = "시작할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		Curriculum curriculum = service().startStudy(new StartCurriculumCommand(authenticatedUserId(authentication), groupId));
		return CurriculumResponse.from(curriculum);
	}

	@Operation(
		summary = "활성 커리큘럼 조회",
		description = "그룹 멤버가 현재 활성화된 커리큘럼의 제목, 총 주차 수, 온보딩 요약, 상태를 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "활성 커리큘럼 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹의 멤버가 아니어서 조회할 수 없음"),
		@ApiResponse(responseCode = "404", description = "그룹 또는 활성 커리큘럼을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "커리큘럼 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/curriculum")
	CurriculumResponse getCurriculum(
		Authentication authentication,
		@Parameter(description = "커리큘럼을 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		Curriculum curriculum = service().getCurriculum(new GetCurriculumQuery(authenticatedUserId(authentication), groupId));
		return CurriculumResponse.from(curriculum);
	}

	@Operation(
		summary = "현재 주차 조회",
		description = "그룹 멤버가 오늘 기준으로 진행해야 하는 현재 커리큘럼 주차의 목표와 기간을 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "현재 주차 정보 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹의 멤버가 아니어서 조회할 수 없음"),
		@ApiResponse(responseCode = "404", description = "그룹 또는 현재 주차를 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "커리큘럼 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/weeks/current")
	CurriculumWeekResponse getCurrentWeek(
		Authentication authentication,
		@Parameter(description = "현재 주차를 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		CurriculumWeek week = service().getCurrentWeek(new GetCurrentWeekQuery(authenticatedUserId(authentication), groupId));
		return CurriculumWeekResponse.from(week);
	}

	@Operation(
		summary = "주차 과제 목록 조회",
		description = "그룹 멤버가 특정 커리큘럼 주차에 배정된 과제 목록을 표시 순서대로 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "주차 과제 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "해당 주차가 속한 그룹의 멤버가 아니어서 조회할 수 없음"),
		@ApiResponse(responseCode = "404", description = "주차를 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "커리큘럼 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/weeks/{weekId}/tasks")
	List<WeeklyTaskResponse> listWeeklyTasks(
		Authentication authentication,
		@Parameter(description = "과제 목록을 조회할 커리큘럼 주차 UUID입니다.", required = true)
		@PathVariable UUID weekId
	) {
		return service().listWeeklyTasks(new ListWeeklyTasksQuery(authenticatedUserId(authentication), weekId)).stream()
			.map(WeeklyTaskResponse::from)
			.toList();
	}


	@Operation(
		summary = "내 주차 진행 상태 저장",
		description = "특정 주차에 대한 내 진행 상태와 완료/미완료 메모를 저장하고 최신 진행 상태를 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "내 주차 진행 상태가 저장되고 최신 상태 반환"),
		@ApiResponse(responseCode = "400", description = "상태 값이 누락되었거나 요청 형식이 올바르지 않음"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "해당 주차가 속한 그룹의 멤버가 아니어서 저장할 수 없음"),
		@ApiResponse(responseCode = "404", description = "주차를 찾을 수 없음"),
		@ApiResponse(responseCode = "409", description = "상태 전환 규칙을 만족하지 못함"),
		@ApiResponse(responseCode = "503", description = "커리큘럼 서비스가 아직 구성되지 않음")
	})

	@GetMapping(ApiPaths.V1 + "/weeks/{weekId}/progress/me")
	MemberWeekProgressResponse getMyWeekProgress(Authentication authentication, @PathVariable UUID weekId) {
		MemberWeekProgress progress = service().getMyWeekProgress(new GetWeekProgressQuery(authenticatedUserId(authentication), weekId));
		return MemberWeekProgressResponse.from(progress);
	}
	@PutMapping(ApiPaths.V1 + "/weeks/{weekId}/progress/me")
	MemberWeekProgressResponse updateMyWeekProgress(
		Authentication authentication,
		@Parameter(description = "진행 상태를 저장할 커리큘럼 주차 UUID입니다.", required = true)
		@PathVariable UUID weekId,
		@Valid @RequestBody UpdateWeekProgressRequest request
	) {
		MemberWeekProgress progress = service().updateMyWeekProgress(request.toCommand(authenticatedUserId(authentication), weekId));
		return MemberWeekProgressResponse.from(progress);
	}

	@Operation(
		summary = "내 과제 완료 상태 저장",
		description = "특정 과제에 대해 완료, 미완료, 건너뜀 상태와 관련 메모/증거 URL을 저장하고 최신 완료 상태를 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "내 과제 완료 상태가 저장되고 최신 상태 반환"),
		@ApiResponse(responseCode = "400", description = "상태 값이 누락되었거나 요청 형식이 올바르지 않음"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "해당 과제가 속한 그룹의 멤버가 아니어서 저장할 수 없음"),
		@ApiResponse(responseCode = "404", description = "과제를 찾을 수 없음"),
		@ApiResponse(responseCode = "409", description = "과제 상태 전환 규칙을 만족하지 못함"),
		@ApiResponse(responseCode = "503", description = "커리큘럼 서비스가 아직 구성되지 않음")
	})
	@PostMapping(ApiPaths.V1 + "/tasks/{taskId}/completion/me")
	TaskCompletionResponse completeTask(
		Authentication authentication,
		@Parameter(description = "완료 상태를 저장할 주차 과제 UUID입니다.", required = true)
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

	@Schema(description = "그룹 시작 후 생성된 커리큘럼 요약 응답입니다.")
	private record CurriculumResponse(
		@Schema(description = "커리큘럼 UUID입니다.", example = "018f6f55-8a81-75cf-ae3d-372699f6f5e8")
		UUID id,
		@Schema(description = "커리큘럼이 속한 스터디 그룹 UUID입니다.", example = "018f6f55-6fb1-7d62-a711-25f7c6d16a28")
		UUID groupId,
		@Schema(description = "커리큘럼 제목입니다.", example = "Spring Boot 백엔드 인터뷰 6주 완성")
		String title,
		@Schema(description = "커리큘럼 전체 주차 수입니다.", example = "6")
		int totalWeeks,
		@Schema(description = "멤버 온보딩 응답을 바탕으로 정리한 커리큘럼 입력 요약입니다.", example = "{\"keywords\":[\"JPA\",\"Security\"],\"preferredTasks\":[\"PRACTICE\"]}")
		Map<String, Object> onboardingSummary,
		@Schema(description = "커리큘럼 생성/진행 상태입니다.", example = "ACTIVE")
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

	@Schema(description = "커리큘럼의 단일 주차 응답입니다.")
	private record CurriculumWeekResponse(
		@Schema(description = "커리큘럼 주차 UUID입니다.", example = "018f6f55-8bf2-78d9-a332-6e74b1484520")
		UUID id,
		@Schema(description = "상위 커리큘럼 UUID입니다.", example = "018f6f55-8a81-75cf-ae3d-372699f6f5e8")
		UUID curriculumId,
		@Schema(description = "커리큘럼 안에서의 주차 번호입니다.", example = "1")
		int weekNumber,
		@Schema(description = "주차 제목입니다.", example = "JPA 엔티티 매핑과 연관관계")
		String title,
		@Schema(description = "이번 주 학습 목표입니다.", example = "엔티티 생명주기와 연관관계 매핑을 코드로 설명할 수 있다.")
		String sprintGoal,
		@Schema(description = "주차 진행 상태입니다.", example = "ACTIVE")
		CurriculumWeekStatus status,
		@Schema(description = "주차 시작 시각입니다.", example = "2026-05-18T00:00:00Z")
		Instant startsAt,
		@Schema(description = "주차 종료 시각입니다.", example = "2026-05-24T23:59:59Z")
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

	@Schema(description = "주차에 배정된 단일 학습 과제 응답입니다.")
	private record WeeklyTaskResponse(
		@Schema(description = "주차 과제 UUID입니다.", example = "018f6f55-8d26-73ed-828f-b955fbd6328a")
		UUID id,
		@Schema(description = "과제가 속한 커리큘럼 주차 UUID입니다.", example = "018f6f55-8bf2-78d9-a332-6e74b1484520")
		UUID curriculumWeekId,
		@Schema(description = "주차 안에서의 표시 순서입니다.", example = "1")
		int displayOrder,
		@Schema(description = "과제 유형입니다.", example = "PRACTICE")
		WeeklyTaskType taskType,
		@Schema(description = "과제 제목입니다.", example = "연관관계 매핑 실습")
		String title,
		@Schema(description = "과제 설명입니다.", example = "회원-주문 예제로 단방향/양방향 매핑을 구현합니다.")
		String description,
		@Schema(description = "필수 수행 과제 여부입니다.", example = "true")
		boolean required,
		@Schema(description = "과제 마감 시각입니다.", example = "2026-05-24T23:59:59Z")
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

	@Schema(description = "내 주차 진행 상태를 저장하기 위한 요청입니다.")
	private record UpdateWeekProgressRequest(
		@Schema(description = "저장할 주차 진행 상태입니다.", example = "IN_PROGRESS")
		@NotNull
		MemberWeekProgressStatus status,
		@Schema(description = "완료 처리 시 남기는 메모입니다.", example = "JPA 기본 실습을 완료했습니다.")
		String completionNote,
		@Schema(description = "미완료 상태일 때 남기는 사유입니다.", example = "개인 일정으로 실습을 마무리하지 못했습니다.")
		String incompleteReason
	) {

		UpdateWeekProgressCommand toCommand(UUID authenticatedUserId, UUID weekId) {
			return new UpdateWeekProgressCommand(authenticatedUserId, weekId, status, completionNote, incompleteReason);
		}
	}

	// completionNote is intentionally omitted to match the locked OpenAPI MemberWeekProgressResponse.
	@Schema(description = "내 주차 진행 상태 저장 결과입니다.")
	private record MemberWeekProgressResponse(
		@Schema(description = "멤버 주차 진행 UUID입니다.", example = "018f6f55-8e42-7183-b6f3-becf5670dfcb")
		UUID id,
		@Schema(description = "현재 주차 진행 상태입니다.", example = "IN_PROGRESS")
		MemberWeekProgressStatus status,
		@Schema(description = "완료 상태가 된 시각입니다. 완료 전이면 null입니다.", example = "2026-05-24T10:15:30Z")
		Instant completedAt,
		@Schema(description = "미완료 상태일 때 제출한 사유입니다.", example = "개인 일정으로 실습을 마무리하지 못했습니다.")
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

	@Schema(description = "내 과제 완료 상태를 저장하기 위한 요청입니다.")
	private record TaskCompletionRequest(
		@Schema(description = "저장할 과제 완료 상태입니다.", example = "DONE")
		@NotNull
		TaskCompletionStatus status,
		@Schema(description = "완료 상태일 때 남기는 메모입니다.", example = "실습 코드를 GitHub에 정리했습니다.")
		String completionNote,
		@Schema(description = "미완료 상태일 때 남기는 사유입니다.", example = "테스트 작성까지 완료하지 못했습니다.")
		String incompleteReason,
		@Schema(description = "완료 증빙 URL입니다.", example = "https://github.com/example/study/blob/main/week1.md")
		String evidenceUrl
	) {

		CompleteTaskCommand toCommand(UUID authenticatedUserId, UUID taskId) {
			return new CompleteTaskCommand(authenticatedUserId, taskId, status, completionNote, incompleteReason, evidenceUrl);
		}
	}

	@Schema(description = "내 과제 완료 상태 저장 결과입니다.")
	private record TaskCompletionResponse(
		@Schema(description = "과제 완료 기록 UUID입니다.", example = "018f6f55-8f6c-7334-a781-84152e57e4f4")
		UUID id,
		@Schema(description = "완료 상태가 저장된 주차 과제 UUID입니다.", example = "018f6f55-8d26-73ed-828f-b955fbd6328a")
		UUID taskId,
		@Schema(description = "현재 과제 완료 상태입니다.", example = "DONE")
		TaskCompletionStatus status,
		@Schema(description = "완료 상태가 된 시각입니다. 완료 전이면 null입니다.", example = "2026-05-24T10:20:30Z")
		Instant completedAt,
		@Schema(description = "미완료 사유가 최초 제출된 시각입니다. 미완료가 아니면 null입니다.", example = "2026-05-24T10:30:30Z")
		Instant reasonSubmittedAt,
		@Schema(description = "완료 상태일 때 제출한 메모입니다.", example = "실습 코드를 GitHub에 정리했습니다.")
		String completionNote,
		@Schema(description = "미완료 상태일 때 제출한 사유입니다.", example = "테스트 작성까지 완료하지 못했습니다.")
		String incompleteReason,
		@Schema(description = "완료 증빙 URL입니다.", example = "https://github.com/example/study/blob/main/week1.md")
		String evidenceUrl
	) {

		private static TaskCompletionResponse from(TaskCompletion completion) {
			return new TaskCompletionResponse(
				completion.id(),
				completion.weeklyTaskId(),
				completion.status(),
				completion.completedAt(),
				completion.reasonSubmittedAt(),
				completion.completionNote(),
				completion.incompleteReason(),
				completion.evidenceUrl()
			);
		}
	}
}
