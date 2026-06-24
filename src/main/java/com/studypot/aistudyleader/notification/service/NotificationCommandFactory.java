package com.studypot.aistudyleader.notification.service;

import com.studypot.aistudyleader.notification.domain.NotificationRelatedResources;
import com.studypot.aistudyleader.notification.domain.NotificationType;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class NotificationCommandFactory {

	private NotificationCommandFactory() {
	}

	static CreateNotificationCommand onboardingRequested(UUID groupId, UUID recipientUserId) {
		return new CreateNotificationCommand(
			groupId,
			recipientUserId,
			null,
			NotificationType.ONBOARDING_REQUESTED,
			"idempotency:notification:onboarding-requested:group:%s:recipient:%s".formatted(groupId, recipientUserId),
			"온보딩을 작성해 주세요",
			"스터디 시작 전 온보딩을 완료해 주세요.",
			payload("/groups/%s/onboarding".formatted(groupId), Map.of("groupId", groupId.toString())),
			null
		);
	}

	static CreateNotificationCommand onboardingSubmitted(UUID groupId, UUID recipientUserId, UUID submitterMemberId) {
		Objects.requireNonNull(submitterMemberId, "submitterMemberId must not be null");
		return new CreateNotificationCommand(
			groupId,
			recipientUserId,
			null,
			NotificationType.ONBOARDING_SUBMITTED,
			"idempotency:notification:onboarding-submitted:group:%s:member:%s:recipient:%s".formatted(groupId, submitterMemberId, recipientUserId),
			"팀원이 온보딩을 완료했어요",
			"한 팀원이 온보딩을 마쳤어요. 팀원 현황을 확인해 주세요.",
			payload("/groups/%s".formatted(groupId), Map.of("groupId", groupId.toString())),
			null
		);
	}

	static CreateNotificationCommand groupDeleted(UUID groupId, UUID recipientUserId, String groupName) {
		String safeName = (groupName == null || groupName.isBlank()) ? "스터디 그룹" : groupName.strip();
		return new CreateNotificationCommand(
			groupId,
			recipientUserId,
			null,
			NotificationType.GROUP_DELETED,
			"idempotency:notification:group-deleted:group:%s:recipient:%s".formatted(groupId, recipientUserId),
			"스터디 그룹이 삭제됐어요",
			"'%s' 그룹이 그룹장에 의해 삭제됐어요.".formatted(safeName),
			payload("/groups", Map.of("groupId", groupId.toString())),
			null
		);
	}

	static CreateNotificationCommand memberJoined(UUID groupId, UUID recipientUserId, UUID joinedUserId) {
		Objects.requireNonNull(joinedUserId, "joinedUserId must not be null");
		return new CreateNotificationCommand(
			groupId,
			recipientUserId,
			null,
			NotificationType.MEMBER_JOINED,
			"idempotency:notification:member-joined:group:%s:member:%s:recipient:%s".formatted(groupId, joinedUserId, recipientUserId),
			"새로운 멤버가 참여했어요",
			"새 멤버가 그룹에 참여했어요. 팀원 목록에서 확인해 주세요.",
			payload("/groups/%s".formatted(groupId), Map.of("groupId", groupId.toString(), "joinedUserId", joinedUserId.toString())),
			null
		);
	}

	static CreateNotificationCommand onboardingCompleted(UUID groupId, UUID recipientUserId) {
		return new CreateNotificationCommand(
			groupId,
			recipientUserId,
			null,
			NotificationType.ONBOARDING_COMPLETED,
			"idempotency:notification:onboarding-completed:group:%s:recipient:%s".formatted(groupId, recipientUserId),
			"모든 팀원이 온보딩을 완료했어요",
			"이제 스터디를 시작할 수 있어요.",
			payload("/groups/%s".formatted(groupId), Map.of("groupId", groupId.toString())),
			null
		);
	}

	static CreateNotificationCommand weekStarted(
		UUID groupId,
		UUID recipientUserId,
		UUID weekId,
		int weekNumber,
		String weekTitle
	) {
		if (weekNumber <= 0) {
			throw new IllegalArgumentException("weekNumber must be positive.");
		}
		return new CreateNotificationCommand(
			groupId,
			recipientUserId,
			new NotificationRelatedResources(null, weekId, null, null),
			NotificationType.WEEK_STARTED,
			"idempotency:notification:week-started:week:%s:recipient:%s".formatted(weekId, recipientUserId),
			"%d주차 학습이 시작됐어요".formatted(weekNumber),
			weekTitle + " todo를 확인해 주세요.",
			payload("/weeks/%s/tasks".formatted(weekId), Map.of(
				"groupId", groupId.toString(),
				"weekId", weekId.toString(),
				"weekNumber", weekNumber,
				"weekTitle", weekTitle
			)),
			null
		);
	}

	static CreateNotificationCommand noticePosted(
		UUID groupId,
		UUID recipientUserId,
		UUID postId,
		String title
	) {
		Objects.requireNonNull(postId, "postId must not be null");
		String safeTitle = requireText(title, "title");
		return new CreateNotificationCommand(
			groupId,
			recipientUserId,
			new NotificationRelatedResources(null, null, null, null),
			NotificationType.NOTICE_POSTED,
			"idempotency:notification:notice-posted:post:%s:recipient:%s".formatted(postId, recipientUserId),
			"새 공지가 등록됐어요",
			safeTitle,
			payload("/groups/%s/posts/%s".formatted(groupId, postId), Map.of(
				"groupId", groupId.toString(),
				"postId", postId.toString(),
				"title", safeTitle
			)),
			null
		);
	}

	static CreateNotificationCommand leaderReportPosted(
		UUID groupId,
		UUID recipientUserId,
		UUID postId,
		String title
	) {
		Objects.requireNonNull(postId, "postId must not be null");
		String safeTitle = requireText(title, "title");
		return new CreateNotificationCommand(
			groupId,
			recipientUserId,
			new NotificationRelatedResources(null, null, null, null),
			NotificationType.LEADER_REPORT_POSTED,
			"idempotency:notification:leader-report-posted:post:%s:recipient:%s".formatted(postId, recipientUserId),
			"새 팀장 리포트가 올라왔어요",
			safeTitle,
			payload("/groups/%s/posts/%s".formatted(groupId, postId), Map.of(
				"groupId", groupId.toString(),
				"postId", postId.toString(),
				"title", safeTitle
			)),
			null
		);
	}

	static CreateNotificationCommand studyCompleted(
		UUID groupId,
		UUID recipientUserId,
		String groupName
	) {
		String safeName = requireText(groupName, "groupName");
		return new CreateNotificationCommand(
			groupId,
			recipientUserId,
			new NotificationRelatedResources(null, null, null, null),
			NotificationType.STUDY_COMPLETED,
			"idempotency:notification:study-completed:group:%s:recipient:%s".formatted(groupId, recipientUserId),
			"스터디를 완주했어요 🎉",
			safeName,
			payload("/groups/%s".formatted(groupId), Map.of(
				"groupId", groupId.toString(),
				"groupName", safeName
			)),
			null
		);
	}

	static CreateNotificationCommand taskNotification(
		NotificationType type,
		UUID groupId,
		UUID recipientUserId,
		UUID weekId,
		UUID taskCompletionId,
		String taskTitle,
		String title,
		String body,
		Map<String, Object> extraPayload
	) {
		Objects.requireNonNull(taskCompletionId, "taskCompletionId must not be null");
		String safeTaskTitle = requireText(taskTitle, "taskTitle");
		Map<String, Object> values = new LinkedHashMap<>(extraPayload);
		values.put("groupId", groupId.toString());
		if (weekId != null) {
			values.put("weekId", weekId.toString());
		}
		values.put("taskCompletionId", taskCompletionId.toString());
		values.put("taskTitle", safeTaskTitle);
		return new CreateNotificationCommand(
			groupId,
			recipientUserId,
			new NotificationRelatedResources(null, weekId, taskCompletionId, null),
			type,
			"idempotency:notification:%s:task-completion:%s:recipient:%s".formatted(
				type.name().toLowerCase(Locale.ROOT),
				taskCompletionId,
				recipientUserId
			),
			title,
			body,
			payload("/task-completions/%s".formatted(taskCompletionId), values),
			null
		);
	}

	static CreateNotificationCommand retrospectiveNotification(
		NotificationType type,
		UUID groupId,
		UUID recipientUserId,
		UUID retrospectiveId,
		UUID weekId,
		String title,
		String body
	) {
		Objects.requireNonNull(retrospectiveId, "retrospectiveId must not be null");
		Objects.requireNonNull(weekId, "weekId must not be null");
		return new CreateNotificationCommand(
			groupId,
			recipientUserId,
			new NotificationRelatedResources(null, weekId, null, retrospectiveId),
			type,
			"idempotency:notification:%s:retrospective:%s:recipient:%s".formatted(
				type.name().toLowerCase(Locale.ROOT),
				retrospectiveId,
				recipientUserId
			),
			title,
			body,
			payload("/weeks/%s/retrospectives/me".formatted(weekId), Map.of(
				"groupId", groupId.toString(),
				"weekId", weekId.toString(),
				"retrospectiveId", retrospectiveId.toString()
			)),
			null
		);
	}

	static CreateNotificationCommand retrospectiveReminder(
		UUID groupId,
		UUID recipientUserId,
		UUID weekId,
		String title,
		String body
	) {
		Objects.requireNonNull(weekId, "weekId must not be null");
		return new CreateNotificationCommand(
			groupId,
			recipientUserId,
			new NotificationRelatedResources(null, weekId, null, null),
			NotificationType.RETROSPECTIVE_REMINDER,
			"idempotency:notification:retrospective_reminder:week:%s:recipient:%s".formatted(
				weekId,
				recipientUserId
			),
			title,
			body,
			payload("/weeks/%s/retrospectives/me".formatted(weekId), Map.of(
				"groupId", groupId.toString(),
				"weekId", weekId.toString()
			)),
			null
		);
	}

	// 리포트 생성 직전(마감 +20분 무렵)에 아직 회고를 안 쓴 멤버에게 보내는 마지막 리마인더.
	// 1시간 전 리마인더와 멱등키를 분리(:final)해 같은 주차에 두 알림이 따로 발송되도록 한다.
	static CreateNotificationCommand retrospectiveFinalReminder(
		UUID groupId,
		UUID recipientUserId,
		UUID weekId,
		String title,
		String body
	) {
		Objects.requireNonNull(weekId, "weekId must not be null");
		return new CreateNotificationCommand(
			groupId,
			recipientUserId,
			new NotificationRelatedResources(null, weekId, null, null),
			NotificationType.RETROSPECTIVE_REMINDER,
			"idempotency:notification:retrospective_reminder:final:week:%s:recipient:%s".formatted(
				weekId,
				recipientUserId
			),
			title,
			body,
			payload("/weeks/%s/retrospectives/me".formatted(weekId), Map.of(
				"groupId", groupId.toString(),
				"weekId", weekId.toString()
			)),
			null
		);
	}

	private static Map<String, Object> payload(String deepLink, Map<String, Object> values) {
		Map<String, Object> payload = new LinkedHashMap<>(values);
		payload.put("deepLink", deepLink);
		return Map.copyOf(payload);
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank.");
		}
		return value.strip();
	}
}
