package com.studypot.aistudyleader.onboarding.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.onboarding.domain.GroupOnboardingResponse;
import com.studypot.aistudyleader.onboarding.domain.GroupOnboardingStatus;
import com.studypot.aistudyleader.onboarding.domain.MemberAvailabilitySlot;
import com.studypot.aistudyleader.onboarding.service.AvailabilitySlotCommand;
import com.studypot.aistudyleader.onboarding.service.GetMyOnboardingQuery;
import com.studypot.aistudyleader.onboarding.service.OnboardingService;
import com.studypot.aistudyleader.onboarding.service.OnboardingServiceUnavailableException;
import com.studypot.aistudyleader.onboarding.service.SubmitMyOnboardingCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "온보딩", description = "그룹 참여자가 전체 실력, 추가 메모, 가능한 시간을 제출하는 API입니다.")
@RestController
@RequiredArgsConstructor
class OnboardingController {

	private final ObjectProvider<OnboardingService> onboardingService;

	@Operation(
		summary = "내 온보딩 응답 조회",
		description = "인증된 그룹 멤버가 제출한 자신의 온보딩 응답과 가능 시간 목록을 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "내 온보딩 응답 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹의 멤버가 아니어서 조회할 수 없음"),
		@ApiResponse(responseCode = "404", description = "그룹 또는 멤버 온보딩 응답을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "온보딩 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/onboarding/me")
	OnboardingResponse getMyOnboarding(
		Authentication authentication,
		@Parameter(description = "온보딩 응답을 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		GroupOnboardingResponse response = service().getMyResponse(new GetMyOnboardingQuery(authenticatedUserId(authentication), groupId));
		return OnboardingResponse.from(response);
	}

	@Operation(
		summary = "내 온보딩 응답 제출",
		description = "전체 실력, 추가 메모, 가능 시간 목록을 제출합니다. 제출 후에는 그룹 시작과 커리큘럼 생성의 입력으로 사용됩니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "온보딩 응답이 제출 상태로 저장되고 제출 시각이 포함된 응답 반환"),
		@ApiResponse(responseCode = "400", description = "필수 값이 누락되었거나 시간 형식이 올바르지 않음"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹의 멤버가 아니어서 제출할 수 없음"),
		@ApiResponse(responseCode = "409", description = "이미 제출되어 다시 제출할 수 없음"),
		@ApiResponse(responseCode = "503", description = "온보딩 서비스가 아직 구성되지 않음")
	})
	@PostMapping(ApiPaths.V1 + "/groups/{groupId}/onboarding/me")
	OnboardingResponse submitMyOnboarding(
		Authentication authentication,
		@Parameter(description = "온보딩 응답을 제출할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Valid @RequestBody SubmitOnboardingRequest request
	) {
		GroupOnboardingResponse response = service().submitMyOnboarding(request.toCommand(authenticatedUserId(authentication), groupId));
		return OnboardingResponse.from(response);
	}

	private OnboardingService service() {
		return onboardingService.getIfAvailable(() -> {
			throw new OnboardingServiceUnavailableException("onboarding service is not configured.");
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

	@Schema(description = "그룹 멤버의 온보딩 제출 요청입니다.")
	private record SubmitOnboardingRequest(
		@Schema(description = "스터디 주제에 대한 전체 자기평가 실력입니다. 1은 입문, 5는 능숙입니다.", example = "3")
		@NotNull
		@Min(1)
		@Max(5)
		Integer skillLevel,
		@Schema(description = "스터디 운영자 또는 AI 팀장에게 전달할 추가 메모입니다.", example = "실습 과제 위주로 진행하고 싶습니다.")
		String additionalNote,
		@Schema(description = "멤버가 스터디에 참여 가능한 요일/시간대 목록입니다.")
		@NotNull
		List<@NotNull @Valid AvailabilitySlotRequest> availabilitySlots
	) {

		SubmitMyOnboardingCommand toCommand(UUID authenticatedUserId, UUID groupId) {
			return new SubmitMyOnboardingCommand(
				authenticatedUserId,
				groupId,
				skillLevel,
				additionalNote,
				availabilitySlots.stream()
					.map(AvailabilitySlotRequest::toCommand)
					.toList()
			);
		}
	}

	@Schema(description = "멤버가 참여 가능한 단일 요일/시간대 요청입니다.")
	private record AvailabilitySlotRequest(
		@Schema(description = "요일 값입니다. 0은 일요일, 6은 토요일입니다.", example = "2")
		@NotNull
		@Min(0)
		@Max(6)
		Integer dayOfWeek,
		@Schema(description = "가능 시간 시작 시각입니다. HH:mm 24시간 형식을 사용합니다.", example = "20:00")
		@NotBlank
		@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "must use HH:mm format")
		String startTime,
		@Schema(description = "가능 시간 종료 시각입니다. HH:mm 24시간 형식을 사용합니다.", example = "22:00")
		@NotBlank
		@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "must use HH:mm format")
		String endTime,
		@Schema(description = "IANA timezone ID입니다.", example = "Asia/Seoul")
		@NotBlank
		@ValidTimezone
		String timezone
	) {

		private AvailabilitySlotCommand toCommand() {
			return new AvailabilitySlotCommand(dayOfWeek, startTime, endTime, timezone);
		}
	}

	@Schema(description = "그룹 멤버의 온보딩 응답 조회/제출 결과입니다.")
	private record OnboardingResponse(
		@Schema(description = "온보딩 응답 UUID입니다.", example = "018f6f55-79ae-7cdb-85a7-34184b15fd12")
		UUID id,
		@Schema(description = "온보딩이 속한 스터디 그룹 UUID입니다.", example = "018f6f55-6fb1-7d62-a711-25f7c6d16a28")
		UUID groupId,
		@Schema(description = "온보딩을 작성한 그룹 멤버 UUID입니다.", example = "018f6f55-75e9-78d2-9f5c-598945b93400")
		UUID memberId,
		@Schema(description = "스터디 주제에 대한 전체 자기평가 실력입니다.", example = "3")
		int skillLevel,
		@Schema(description = "멤버가 남긴 추가 메모입니다.", example = "실습 과제 위주로 진행하고 싶습니다.")
		String additionalNote,
		@Schema(description = "저장된 참여 가능 시간 목록입니다.")
		List<AvailabilitySlotResponse> availabilitySlots,
		@Schema(description = "온보딩 응답 상태입니다.", example = "DRAFT")
		GroupOnboardingStatus status,
		@Schema(description = "제출 완료 시각입니다. 아직 제출하지 않았다면 null입니다.", example = "2026-05-18T11:30:00Z")
		Instant submittedAt
	) {

		private static OnboardingResponse from(GroupOnboardingResponse response) {
			return new OnboardingResponse(
				response.id(),
				response.groupId(),
				response.memberId(),
				response.skillLevel(),
				response.additionalNote().orElse(null),
				response.availabilitySlots().stream()
					.map(AvailabilitySlotResponse::from)
					.toList(),
				response.status(),
				response.submittedAt().orElse(null)
			);
		}
	}

	@Schema(description = "멤버가 참여 가능한 단일 요일/시간대 응답입니다.")
	private record AvailabilitySlotResponse(
		@Schema(description = "요일 값입니다. 0은 일요일, 6은 토요일입니다.", example = "2")
		int dayOfWeek,
		@Schema(description = "가능 시간 시작 시각입니다.", example = "20:00")
		String startTime,
		@Schema(description = "가능 시간 종료 시각입니다.", example = "22:00")
		String endTime,
		@Schema(description = "IANA timezone ID입니다.", example = "Asia/Seoul")
		String timezone
	) {

		private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

		private static AvailabilitySlotResponse from(MemberAvailabilitySlot slot) {
			return new AvailabilitySlotResponse(
				slot.dayOfWeek(),
				format(slot.startTime()),
				format(slot.endTime()),
				slot.timezone()
			);
		}

		private static String format(LocalTime time) {
			return time.format(TIME_FORMAT);
		}
	}
}
