package com.studypot.aistudyleader.curriculum.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.curriculum.domain.Curriculum;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStatus;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeek;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeekStatus;
import com.studypot.aistudyleader.curriculum.domain.CurrentLearningActivity;
import com.studypot.aistudyleader.curriculum.domain.GroupActivityHeatmap;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgress;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgressStatus;
import com.studypot.aistudyleader.curriculum.domain.TaskCompletion;
import com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTask;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import com.studypot.aistudyleader.curriculum.service.CurriculumService;
import com.studypot.aistudyleader.curriculum.service.CurriculumServiceUnavailableException;
import com.studypot.aistudyleader.curriculum.service.NextWeekPlanService;
import com.studypot.aistudyleader.curriculum.service.RegenerateNextWeekCommand;
import com.studypot.aistudyleader.curriculum.service.CompleteTaskCommand;
import com.studypot.aistudyleader.curriculum.service.GetCurrentWeekQuery;
import com.studypot.aistudyleader.curriculum.service.GetWeekByIdQuery;
import com.studypot.aistudyleader.curriculum.service.GetCurriculumQuery;
import com.studypot.aistudyleader.curriculum.service.GetGroupActivityHeatmapQuery;
import com.studypot.aistudyleader.curriculum.service.GetLearningActivityQuery;
import com.studypot.aistudyleader.curriculum.service.GetWeekProgressQuery;
import com.studypot.aistudyleader.curriculum.service.ListWeeklyTasksQuery;
import com.studypot.aistudyleader.curriculum.service.WeeklyTaskWithCompletion;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "커리큘럼/주차 Todo", description = "그룹 시작 후 커리큘럼, 현재 주차, 주차 과제, 멤버 진행 상태와 과제 완료를 다루는 API입니다.")
@RestController
@RequiredArgsConstructor
class CurriculumController {

	private final ObjectProvider<CurriculumService> curriculumService;
	private final ObjectProvider<NextWeekPlanService> nextWeekPlanService;

	@Operation(
		summary = "리포트 기반 다음 주차 TODO 재생성",
		description = "그룹장이 직전 주차의 학습 리포트를 바탕으로 다음 주차의 TODO와 회고 프롬프트를 AI로 재생성합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "재생성된 다음 주차 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "그룹장이 아니어서 재생성할 수 없음"),
		@ApiResponse(responseCode = "404", description = "주차/다음 주차/리포트를 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "커리큘럼 서비스가 아직 구성되지 않음")
	})
	@PostMapping(ApiPaths.V1 + "/groups/{groupId}/weeks/{weekId}/next-week-plan")
	CurriculumWeekResponse regenerateNextWeek(
		Authentication authentication,
		@Parameter(description = "리포트가 작성된 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Parameter(description = "직전(리포트 대상) 주차 UUID입니다. 이 주차의 다음 주차가 재생성됩니다.", required = true)
		@PathVariable UUID weekId
	) {
		CurriculumWeek week = nextWeekService().regenerateNextWeek(
			new RegenerateNextWeekCommand(authenticatedUserId(authentication), groupId, weekId)
		);
		return CurriculumWeekResponse.from(week);
	}

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
		summary = "커리큘럼 전체 주차 목록 조회",
		description = "그룹 멤버가 현재 활성 커리큘럼의 모든 주차(다음 주차 포함)를 주차 번호 순서로 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "주차 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹의 멤버가 아니어서 조회할 수 없음"),
		@ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "커리큘럼 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/weeks")
	List<CurriculumWeekResponse> listCurriculumWeeks(
		Authentication authentication,
		@Parameter(description = "주차 목록을 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		return service().listCurriculumWeeks(new GetCurrentWeekQuery(authenticatedUserId(authentication), groupId))
			.stream()
			.map(CurriculumWeekResponse::from)
			.toList();
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
		summary = "주차 단건 조회",
		description = "그룹 멤버가 특정 커리큘럼 주차의 목표·기간·회고 질문 등 상세를 조회합니다. (주차 네비게이션용)"
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "주차 정보 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹의 멤버가 아니어서 조회할 수 없음"),
		@ApiResponse(responseCode = "404", description = "주차를 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "커리큘럼 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/weeks/{weekId}")
	CurriculumWeekResponse getWeek(
		Authentication authentication,
		@Parameter(description = "조회할 커리큘럼 주차 UUID입니다.", required = true)
		@PathVariable UUID weekId
	) {
		CurriculumWeek week = service().getWeek(new GetWeekByIdQuery(authenticatedUserId(authentication), weekId));
		return CurriculumWeekResponse.from(week);
	}

	@Operation(
		summary = "그룹홈 현재 학습활동 조회",
		description = "그룹홈에서 현재 주차, 내 주차 진행 상태, 과제 목록과 내 과제 완료 상태를 한 번에 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "현재 학습활동 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "활성 그룹 멤버가 아니어서 조회할 수 없음"),
		@ApiResponse(responseCode = "404", description = "그룹 또는 현재 주차를 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "커리큘럼 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/learning-activity/me")
	CurrentLearningActivityResponse getCurrentLearningActivity(
		Authentication authentication,
		@Parameter(description = "학습활동을 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		CurrentLearningActivity activity = service().getCurrentLearningActivity(
			new GetLearningActivityQuery(authenticatedUserId(authentication), groupId)
		);
		return CurrentLearningActivityResponse.from(activity);
	}

	@Operation(
		summary = "그룹홈 활동 잔디(히트맵) 조회",
		description = "그룹홈 활동 대시보드용으로, 그룹 멤버별 최근 N일(기본 28일) 동안 완료(DONE)한 과제 수를 일자별로 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "활동 히트맵 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "활성 그룹 멤버가 아니어서 조회할 수 없음"),
		@ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "커리큘럼 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/activity-heatmap")
	GroupActivityHeatmapResponse getGroupActivityHeatmap(
		Authentication authentication,
		@Parameter(description = "활동 히트맵을 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Parameter(description = "조회할 기간(일수)입니다. 기본 28, 1~84 범위로 보정됩니다.")
		@RequestParam(name = "days", defaultValue = "28") int days
	) {
		int boundedDays = Math.min(Math.max(days, 1), GetGroupActivityHeatmapQuery.MAX_DAYS);
		GroupActivityHeatmap heatmap = service().getGroupActivityHeatmap(
			new GetGroupActivityHeatmapQuery(authenticatedUserId(authentication), groupId, boundedDays)
		);
		return GroupActivityHeatmapResponse.from(heatmap);
	}

	@Operation(
		summary = "그룹홈 멤버별 활동(잔디) 목록 조회",
		description = "그룹홈 잔디용으로, 그룹 멤버별 최근 28일 동안 완료(DONE)한 과제 수를 일자 배열로 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "멤버별 활동 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "활성 그룹 멤버가 아니어서 조회할 수 없음"),
		@ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "커리큘럼 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/learning-activity")
	List<MemberActivityRowResponse> getGroupMembersActivity(
		Authentication authentication,
		@Parameter(description = "활동 목록을 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		GroupActivityHeatmap heatmap = service().getGroupActivityHeatmap(
			new GetGroupActivityHeatmapQuery(authenticatedUserId(authentication), groupId, GetGroupActivityHeatmapQuery.DEFAULT_DAYS)
		);
		return MemberActivityRowResponse.from(heatmap);
	}

	@Operation(
		summary = "그룹 최근 활동 피드 조회",
		description = "그룹 홈 '최근 활동' 카드용으로, 최근 완료된 과제(누가/무슨 과제/언제)를 최신순으로 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "최근 활동 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "활성 그룹 멤버가 아니어서 조회할 수 없음"),
		@ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "커리큘럼 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/activity-feed")
	List<RecentActivityResponse> getRecentActivityFeed(
		Authentication authentication,
		@Parameter(description = "최근 활동을 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Parameter(description = "조회할 최근 활동 수입니다. 1~50, 기본 8.")
		@RequestParam(defaultValue = "8") int limit
	) {
		return service().getRecentTaskActivity(authenticatedUserId(authentication), groupId, limit)
			.stream()
			.map(RecentActivityResponse::from)
			.toList();
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

	@Operation(
		summary = "내 과제 완료 처리",
		description = "TODO 버튼에서 특정 과제를 완료 상태로 저장하고 최신 완료 상태를 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "완료 상태 저장 후 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "해당 과제가 속한 그룹의 활성 멤버가 아니어서 저장할 수 없음"),
		@ApiResponse(responseCode = "404", description = "과제를 찾을 수 없음"),
		@ApiResponse(responseCode = "409", description = "과제 상태 전환 규칙을 만족하지 못함")
	})
	@PostMapping(ApiPaths.V1 + "/tasks/{taskId}/completion/me/done")
	TaskCompletionResponse markTaskDone(
		Authentication authentication,
		@Parameter(description = "완료 처리할 주차 과제 UUID입니다.", required = true)
		@PathVariable UUID taskId,
		@Valid @RequestBody DoneTaskRequest request
	) {
		TaskCompletion completion = service().completeMyTask(request.toCommand(authenticatedUserId(authentication), taskId));
		return TaskCompletionResponse.from(completion);
	}

	@Operation(
		summary = "내 과제 미완료 처리",
		description = "TODO 버튼에서 특정 과제를 미완료 상태로 저장하고 최신 완료 상태를 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "미완료 상태 저장 후 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "해당 과제가 속한 그룹의 활성 멤버가 아니어서 저장할 수 없음"),
		@ApiResponse(responseCode = "404", description = "과제를 찾을 수 없음"),
		@ApiResponse(responseCode = "409", description = "과제 상태 전환 규칙을 만족하지 못함"),
		@ApiResponse(responseCode = "422", description = "미완료 사유 검증 실패")
	})
	@PostMapping(ApiPaths.V1 + "/tasks/{taskId}/completion/me/incomplete")
	TaskCompletionResponse markTaskIncomplete(
		Authentication authentication,
		@Parameter(description = "미완료 처리할 주차 과제 UUID입니다.", required = true)
		@PathVariable UUID taskId,
		@Valid @RequestBody IncompleteTaskRequest request
	) {
		TaskCompletion completion = service().completeMyTask(request.toCommand(authenticatedUserId(authentication), taskId));
		return TaskCompletionResponse.from(completion);
	}

	@Operation(
		summary = "내 과제 스킵 처리",
		description = "TODO 버튼에서 특정 과제를 건너뜀 상태로 저장하고 최신 완료 상태를 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "건너뜀 상태 저장 후 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "해당 과제가 속한 그룹의 활성 멤버가 아니어서 저장할 수 없음"),
		@ApiResponse(responseCode = "404", description = "과제를 찾을 수 없음"),
		@ApiResponse(responseCode = "409", description = "과제 상태 전환 규칙을 만족하지 못함")
	})
	@PostMapping(ApiPaths.V1 + "/tasks/{taskId}/completion/me/skip")
	TaskCompletionResponse skipTask(
		Authentication authentication,
		@Parameter(description = "건너뜀 처리할 주차 과제 UUID입니다.", required = true)
		@PathVariable UUID taskId
	) {
		TaskCompletion completion = service().completeMyTask(new CompleteTaskCommand(
			authenticatedUserId(authentication),
			taskId,
			TaskCompletionStatus.SKIPPED,
			null,
			null,
			null
		));
		return TaskCompletionResponse.from(completion);
	}

	private CurriculumService service() {
		return curriculumService.getIfAvailable(() -> {
			throw new CurriculumServiceUnavailableException("curriculum service is not configured.");
		});
	}

	private NextWeekPlanService nextWeekService() {
		return nextWeekPlanService.getIfAvailable(() -> {
			throw new CurriculumServiceUnavailableException("next week plan service is not configured.");
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
		@Schema(description = "이번 주차 회고 설문 질문 목록(커리큘럼 생성 시 AI가 생성). 각 질문은 id/text/type(LIKERT_5|TEXT).")
		java.util.List<com.studypot.aistudyleader.curriculum.domain.RetrospectiveQuestion> retrospectiveQuestions,
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
				week.retrospectiveQuestions(),
				week.status(),
				week.startsAt(),
				week.endsAt()
			);
		}
	}

	@Schema(description = "그룹홈 활동 잔디(히트맵) 응답입니다.")
	private record GroupActivityHeatmapResponse(
		@Schema(description = "집계 시작일(포함)입니다.", example = "2026-05-26")
		LocalDate startDate,
		@Schema(description = "집계 종료일(포함)입니다.", example = "2026-06-22")
		LocalDate endDate,
		@Schema(description = "시작일부터 종료일까지 연속된 날짜 목록입니다. 각 멤버의 counts 와 같은 순서입니다.")
		List<LocalDate> days,
		@Schema(description = "멤버별 일자 활동량입니다.")
		List<MemberActivityResponse> members
	) {

		private static GroupActivityHeatmapResponse from(GroupActivityHeatmap heatmap) {
			return new GroupActivityHeatmapResponse(
				heatmap.startDate(),
				heatmap.endDate(),
				heatmap.days(),
				heatmap.members().stream().map(MemberActivityResponse::from).toList()
			);
		}
	}

	@Schema(description = "그룹홈 활동 잔디의 멤버별 행입니다.")
	private record MemberActivityResponse(
		@Schema(description = "그룹 멤버십 UUID입니다.", example = "018f6f55-75e9-78d2-9f5c-598945b93400")
		UUID memberId,
		@Schema(description = "멤버의 사용자 UUID입니다.", example = "018f6f55-6f42-7e11-b479-120c5f2e9d42")
		UUID userId,
		@Schema(description = "그룹 안에서 표시할 이름입니다.", example = "현우")
		String displayName,
		@Schema(description = "사용자 닉네임입니다.", example = "hyunwoo")
		String nickname,
		@Schema(description = "days 순서에 맞춘 일자별 완료(DONE) 과제 수입니다.", example = "[0,1,0,2]")
		List<Integer> counts
	) {

		private static MemberActivityResponse from(GroupActivityHeatmap.MemberActivity member) {
			return new MemberActivityResponse(
				member.memberId(),
				member.userId(),
				member.displayName(),
				member.nickname(),
				member.counts()
			);
		}
	}

	@Schema(description = "그룹홈 잔디용 멤버별 일자 활동 행입니다.")
	private record MemberActivityRowResponse(
		@Schema(description = "그룹 멤버십 UUID입니다.", example = "018f6f55-75e9-78d2-9f5c-598945b93400")
		UUID memberId,
		@Schema(description = "멤버 표시 닉네임입니다.", example = "현우")
		String memberNickname,
		@Schema(description = "일자별 완료(DONE) 과제 수 목록입니다.")
		List<DailyActivityResponse> dailyActivity
	) {

		private static List<MemberActivityRowResponse> from(GroupActivityHeatmap heatmap) {
			List<LocalDate> days = heatmap.days();
			return heatmap.members().stream()
				.map(member -> {
					List<Integer> counts = member.counts();
					List<Integer> todos = member.todoCounts();
					List<Integer> posts = member.postCounts();
					List<DailyActivityResponse> daily = new java.util.ArrayList<>(days.size());
					for (int i = 0; i < days.size(); i++) {
						daily.add(new DailyActivityResponse(
							days.get(i),
							i < counts.size() ? counts.get(i) : 0,
							i < todos.size() ? todos.get(i) : 0,
							i < posts.size() ? posts.get(i) : 0));
					}
					String nickname = member.nickname() != null ? member.nickname() : member.displayName();
					return new MemberActivityRowResponse(member.memberId(), nickname, daily);
				})
				.toList();
		}
	}

	@Schema(description = "잔디의 단일 일자 활동입니다.")
	private record DailyActivityResponse(
		@Schema(description = "활동 일자입니다.", example = "2026-06-22")
		LocalDate date,
		@Schema(description = "그 날 활동 수(완료 과제 + 게시글) 합산입니다.", example = "3")
		int count,
		@Schema(description = "그 날 완료(DONE)한 과제 수입니다.", example = "2")
		int todoCount,
		@Schema(description = "그 날 작성한 게시글 수입니다.", example = "1")
		int postCount
	) {
	}

	@Schema(description = "그룹 최근 활동 피드의 단일 항목입니다.")
	private record RecentActivityResponse(
		@Schema(description = "활동한 그룹 멤버 UUID입니다.", example = "018f6f55-75e9-78d2-9f5c-598945b93400")
		UUID memberId,
		@Schema(description = "멤버 표시 닉네임입니다.", example = "현우")
		String memberNickname,
		@Schema(description = "완료한 과제 제목입니다.", example = "JWT 인증 흐름 다이어그램 그리기")
		String taskTitle,
		@Schema(description = "완료 시각입니다.", example = "2026-06-22T11:30:00Z")
		Instant completedAt
	) {

		private static RecentActivityResponse from(com.studypot.aistudyleader.curriculum.domain.RecentTaskActivity item) {
			return new RecentActivityResponse(item.memberId(), item.memberNickname(), item.taskTitle(), item.completedAt());
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
		Instant dueAt,
		@Schema(description = "인증 멤버의 과제 완료 상태입니다. 아직 어떤 액션도 하지 않았으면 null입니다.")
		TaskCompletionResponse completion
	) {

		private static WeeklyTaskResponse from(WeeklyTask task) {
			return from(task, null);
		}

		private static WeeklyTaskResponse from(WeeklyTaskWithCompletion taskWithCompletion) {
			return from(taskWithCompletion.task(), taskWithCompletion.completion());
		}

		private static WeeklyTaskResponse from(WeeklyTask task, TaskCompletion completion) {
			return new WeeklyTaskResponse(
				task.id(),
				task.curriculumWeekId(),
				task.displayOrder(),
				task.taskType(),
				task.title(),
				task.description(),
				task.required(),
				task.dueAt(),
				completion == null ? null : TaskCompletionResponse.from(completion)
			);
		}
	}

	@Schema(description = "그룹홈에서 사용하는 현재 학습활동 응답입니다.")
	private record CurrentLearningActivityResponse(
		@Schema(description = "스터디 그룹 UUID입니다.", example = "018f6f55-6fb1-7d62-a711-25f7c6d16a28")
		UUID groupId,
		@Schema(description = "현재 진행 중인 커리큘럼 주차입니다.")
		CurriculumWeekResponse currentWeek,
		@Schema(description = "현재 주차에 대한 내 진행 기록입니다. 아직 생성되지 않았다면 null입니다.")
		MemberWeekProgressResponse progress,
		@Schema(description = "현재 주차에 표시할 내 진행 상태입니다. 진행 기록이 없으면 NOT_STARTED입니다.", example = "IN_PROGRESS")
		MemberWeekProgressStatus progressStatus,
		@Schema(description = "현재 주차 과제 완료 요약입니다.")
		LearningActivityTaskCompletionSummaryResponse taskCompletion,
		@Schema(description = "현재 주차 과제와 내 완료 상태 목록입니다.")
		List<LearningActivityTaskResponse> tasks
	) {

		private static CurrentLearningActivityResponse from(CurrentLearningActivity activity) {
			return new CurrentLearningActivityResponse(
				activity.groupId(),
				CurriculumWeekResponse.from(activity.currentWeek()),
				activity.progress().map(MemberWeekProgressResponse::from).orElse(null),
				activity.progressStatus(),
				LearningActivityTaskCompletionSummaryResponse.from(activity.taskCompletion()),
				activity.tasks().stream()
					.map(LearningActivityTaskResponse::from)
					.toList()
			);
		}
	}

	@Schema(description = "현재 학습활동의 단일 과제와 내 완료 상태 응답입니다.")
	private record LearningActivityTaskResponse(
		WeeklyTaskResponse task,
		LearningActivityTaskCompletionResponse completion
	) {

		private static LearningActivityTaskResponse from(CurrentLearningActivity.Task task) {
			return new LearningActivityTaskResponse(
				WeeklyTaskResponse.from(task.task()),
				LearningActivityTaskCompletionResponse.from(task)
			);
		}
	}

	@Schema(description = "현재 학습활동 과제에 대한 내 완료 상태 스냅샷입니다.")
	private record LearningActivityTaskCompletionResponse(
		UUID id,
		UUID taskId,
		TaskCompletionStatus status,
		Instant completedAt,
		Instant reasonSubmittedAt,
		String completionNote,
		String incompleteReason,
		String evidenceUrl
	) {

		private static LearningActivityTaskCompletionResponse from(CurrentLearningActivity.Task activityTask) {
			TaskCompletion completion = activityTask.completion().orElse(null);
			return new LearningActivityTaskCompletionResponse(
				completion == null ? null : completion.id(),
				activityTask.task().id(),
				activityTask.completionStatus(),
				completion == null ? null : completion.completedAt(),
				completion == null ? null : completion.reasonSubmittedAt(),
				completion == null ? null : completion.completionNote(),
				completion == null ? null : completion.incompleteReason(),
				completion == null ? null : completion.evidenceUrl()
			);
		}
	}

	@Schema(description = "현재 학습활동 과제 완료 상태 요약입니다.")
	private record LearningActivityTaskCompletionSummaryResponse(
		int totalCount,
		int doneCount,
		int incompleteCount,
		int skippedCount
	) {

		private static LearningActivityTaskCompletionSummaryResponse from(
			CurrentLearningActivity.TaskCompletionSummary summary
		) {
			return new LearningActivityTaskCompletionSummaryResponse(
				summary.totalCount(),
				summary.doneCount(),
				summary.incompleteCount(),
				summary.skippedCount()
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

	@Schema(description = "과제를 완료 처리하기 위한 요청입니다.")
	private record DoneTaskRequest(
		@Schema(description = "완료 상태일 때 남기는 메모입니다.", example = "실습 코드를 GitHub에 정리했습니다.")
		String completionNote,
		@Schema(description = "완료 증빙 URL입니다.", example = "https://github.com/example/study/blob/main/week1.md")
		String evidenceUrl
	) {

		CompleteTaskCommand toCommand(UUID authenticatedUserId, UUID taskId) {
			return new CompleteTaskCommand(
				authenticatedUserId,
				taskId,
				TaskCompletionStatus.DONE,
				completionNote,
				null,
				evidenceUrl
			);
		}
	}

	@Schema(description = "과제를 미완료 처리하기 위한 요청입니다.")
	private record IncompleteTaskRequest(
		@Schema(description = "미완료 상태일 때 남기는 선택 사유입니다. 비워둘 수 있습니다.", example = "테스트 작성까지 완료하지 못했습니다.")
		String incompleteReason
	) {

		CompleteTaskCommand toCommand(UUID authenticatedUserId, UUID taskId) {
			return new CompleteTaskCommand(
				authenticatedUserId,
				taskId,
				TaskCompletionStatus.INCOMPLETE,
				null,
				incompleteReason,
				null
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
