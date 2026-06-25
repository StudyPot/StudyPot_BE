package com.studypot.aistudyleader.notification.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.notification.domain.Notification;
import com.studypot.aistudyleader.notification.service.ListGroupNotificationsQuery;
import com.studypot.aistudyleader.notification.service.ListMyNotificationsQuery;
import com.studypot.aistudyleader.notification.service.MarkAllMyNotificationsReadCommand;
import com.studypot.aistudyleader.notification.service.MarkNotificationReadCommand;
import com.studypot.aistudyleader.notification.service.NotificationService;
import com.studypot.aistudyleader.notification.service.NotificationServiceUnavailableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "알림", description = "서비스 내부 IN_APP 알림 조회와 읽음 처리를 다루는 API입니다.")
@RestController
@RequiredArgsConstructor
class NotificationController {

	private final ObjectProvider<NotificationService> notificationService;
	private final ObjectProvider<NotificationStreamService> notificationStreamService;

	@Operation(
		summary = "내 인앱 알림 조회",
		description = "인증된 사용자가 자신에게 전달된 서비스 내부 IN_APP 알림을 최신순으로 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "내 알림 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "503", description = "알림 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/users/me/notifications")
	List<NotificationResponse> listMyNotifications(
		Authentication authentication,
		@Parameter(description = "읽지 않은 알림만 조회할지 여부입니다.")
		@RequestParam(defaultValue = "false") boolean unreadOnly,
		@Parameter(description = "향후 커서 페이지네이션 확장을 위한 선택 입력입니다.")
		@RequestParam(required = false) String cursor
	) {
		return service().listMyNotifications(new ListMyNotificationsQuery(authenticatedUserId(authentication), unreadOnly, cursor))
			.stream()
			.map(NotificationResponse::from)
			.toList();
	}

	@Operation(
		summary = "내 인앱 알림 SSE 구독",
		description = "인증된 사용자가 자신에게 새로 전달되는 서비스 내부 IN_APP 알림을 SSE로 구독합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "SSE 연결 시작"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "503", description = "알림 스트림 서비스가 아직 구성되지 않음")
	})
	@GetMapping(path = ApiPaths.V1 + "/users/me/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	SseEmitter subscribeMyNotificationStream(Authentication authentication, HttpServletResponse response) {
		// 리버스 프록시(nginx 등)의 응답 버퍼링을 비활성화해 SSE 이벤트가 실시간으로 전달되도록 한다.
		response.setHeader("X-Accel-Buffering", "no");
		response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
		return streamService().subscribe(authenticatedUserId(authentication));
	}

	@Operation(
		summary = "단일 알림 읽음 처리",
		description = "인증된 사용자가 자신의 IN_APP 알림 하나를 읽음 상태로 변경합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "읽음 처리된 알림 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "다른 사용자의 알림이라 읽음 처리할 수 없음"),
		@ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음"),
		@ApiResponse(responseCode = "409", description = "읽음 처리할 수 없는 알림 상태"),
		@ApiResponse(responseCode = "503", description = "알림 서비스가 아직 구성되지 않음")
	})
	@PostMapping(ApiPaths.V1 + "/notifications/{notificationId}/read")
	NotificationResponse markNotificationRead(
		Authentication authentication,
		@Parameter(description = "읽음 처리할 알림 UUID입니다.", required = true)
		@PathVariable UUID notificationId
	) {
		Notification notification = service().markNotificationRead(
			new MarkNotificationReadCommand(authenticatedUserId(authentication), notificationId)
		);
		return NotificationResponse.from(notification);
	}

	@Operation(
		summary = "내 알림 전체 읽음 처리",
		description = "인증된 사용자가 자신에게 전달된 읽지 않은 IN_APP 알림을 모두 읽음 상태로 변경합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "내 알림 전체 읽음 처리 완료"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "503", description = "알림 서비스가 아직 구성되지 않음")
	})
	@PostMapping(ApiPaths.V1 + "/users/me/notifications/read-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void markAllMyNotificationsRead(Authentication authentication) {
		service().markAllMyNotificationsRead(new MarkAllMyNotificationsReadCommand(authenticatedUserId(authentication)));
	}

	@Operation(
		summary = "그룹 알림 로그 조회",
		description = "그룹장이 그룹에 생성된 IN_APP 알림 로그를 감사 목적으로 최신순 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "그룹 알림 로그 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "그룹장이 아니어서 알림 로그를 조회할 수 없음"),
		@ApiResponse(responseCode = "404", description = "스터디 그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "알림 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/notifications")
	List<NotificationResponse> listGroupNotifications(
		Authentication authentication,
		@Parameter(description = "알림 로그를 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		return service().listGroupNotifications(new ListGroupNotificationsQuery(authenticatedUserId(authentication), groupId))
			.stream()
			.map(NotificationResponse::from)
			.toList();
	}

	private NotificationService service() {
		return notificationService.getIfAvailable(() -> {
			throw new NotificationServiceUnavailableException("notification service is not configured.");
		});
	}

	private NotificationStreamService streamService() {
		return notificationStreamService.getIfAvailable(() -> {
			throw new NotificationServiceUnavailableException("notification stream service is not configured.");
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
}
