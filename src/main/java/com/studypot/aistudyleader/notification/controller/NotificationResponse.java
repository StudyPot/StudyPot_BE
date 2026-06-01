package com.studypot.aistudyleader.notification.controller;

import com.studypot.aistudyleader.notification.domain.Notification;
import com.studypot.aistudyleader.notification.domain.NotificationChannel;
import com.studypot.aistudyleader.notification.domain.NotificationStatus;
import com.studypot.aistudyleader.notification.domain.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "서비스 내부 IN_APP 알림 응답입니다.")
record NotificationResponse(
	@Schema(description = "알림 UUID입니다.", example = "018f6f55-9c4b-74d1-82dd-36ff0e71ca5f")
	UUID id,
	@Schema(description = "알림 유형입니다.", example = "RETROSPECTIVE_READY")
	NotificationType notificationType,
	@Schema(description = "알림 채널입니다.", example = "IN_APP")
	NotificationChannel channel,
	@Schema(description = "알림 제목입니다.", example = "회고 피드백이 준비됐어요")
	String title,
	@Schema(description = "알림 본문입니다.", example = "이번 주 미완료 사유를 바탕으로 AI 팀장 피드백이 생성됐습니다.")
	String body,
	@Schema(description = "알림 상태입니다.", example = "DELIVERED")
	NotificationStatus status,
	@Schema(description = "예약 시각입니다.", example = "2026-05-13T09:00:00Z")
	Instant scheduledAt,
	@Schema(description = "인앱 전달 시각입니다.", example = "2026-05-13T09:01:00Z")
	Instant deliveredAt,
	@Schema(description = "사용자 읽음 시각입니다.", example = "2026-05-13T09:05:00Z")
	Instant readAt
) {

	static NotificationResponse from(Notification notification) {
		return new NotificationResponse(
			notification.id(),
			notification.notificationType(),
			notification.channel(),
			notification.title(),
			notification.body(),
			notification.status(),
			notification.scheduledAt(),
			notification.deliveredAt(),
			notification.readAt()
		);
	}
}
