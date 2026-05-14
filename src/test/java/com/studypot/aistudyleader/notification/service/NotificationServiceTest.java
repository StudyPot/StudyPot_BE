package com.studypot.aistudyleader.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.notification.domain.Notification;
import com.studypot.aistudyleader.notification.domain.NotificationAccessContext;
import com.studypot.aistudyleader.notification.domain.NotificationChannel;
import com.studypot.aistudyleader.notification.domain.NotificationRelatedResources;
import com.studypot.aistudyleader.notification.domain.NotificationStatus;
import com.studypot.aistudyleader.notification.domain.NotificationType;
import com.studypot.aistudyleader.notification.repository.NotificationRepository;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000008101");
	private static final UUID OTHER_USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000008102");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000008103");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000008104");
	private static final UUID NOTIFICATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000008105");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000008106");
	private static final Instant NOW = Instant.parse("2026-05-13T05:20:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Test
	void listMyNotificationsPassesRecipientAndUnreadFilter() {
		FakeRepository repository = new FakeRepository();
		Notification notification = notification(USER_ID, NotificationStatus.DELIVERED, null);
		repository.myNotifications = List.of(notification);
		NotificationService service = new NotificationService(repository, CLOCK);

		List<Notification> result = service.listMyNotifications(new ListMyNotificationsQuery(USER_ID, true, "ignored"));

		assertThat(result).containsExactly(notification);
		assertThat(repository.requestedUserId).isEqualTo(USER_ID);
		assertThat(repository.requestedUnreadOnly).isTrue();
		assertThat(repository.requestedLimit).isEqualTo(100);
	}

	@Test
	void markNotificationReadUpdatesDeliveredRecipientNotification() {
		FakeRepository repository = new FakeRepository();
		repository.notification = notification(USER_ID, NotificationStatus.DELIVERED, null);
		NotificationService service = new NotificationService(repository, CLOCK);

		Notification result = service.markNotificationRead(new MarkNotificationReadCommand(USER_ID, NOTIFICATION_ID));

		assertThat(result.status()).isEqualTo(NotificationStatus.READ);
		assertThat(result.readAt()).isEqualTo(NOW);
		assertThat(repository.updatedNotificationId).isEqualTo(NOTIFICATION_ID);
		assertThat(repository.updatedRecipientUserId).isEqualTo(USER_ID);
		assertThat(repository.updatedReadAt).isEqualTo(NOW);
	}

	@Test
	void markNotificationReadIsIdempotentWhenAlreadyRead() {
		FakeRepository repository = new FakeRepository();
		repository.notification = notification(USER_ID, NotificationStatus.READ, NOW.minusSeconds(10));
		NotificationService service = new NotificationService(repository, CLOCK);

		Notification result = service.markNotificationRead(new MarkNotificationReadCommand(USER_ID, NOTIFICATION_ID));

		assertThat(result.status()).isEqualTo(NotificationStatus.READ);
		assertThat(repository.updatedNotificationId).isNull();
	}

	@Test
	void markNotificationReadRejectsOtherRecipientAndPendingStatus() {
		FakeRepository repository = new FakeRepository();
		repository.notification = notification(OTHER_USER_ID, NotificationStatus.DELIVERED, null);
		NotificationService service = new NotificationService(repository, CLOCK);

		assertThatThrownBy(() -> service.markNotificationRead(new MarkNotificationReadCommand(USER_ID, NOTIFICATION_ID)))
			.isInstanceOf(NotificationAccessDeniedException.class)
			.hasMessage("authenticated user is not the notification recipient.");

		repository.notification = notification(USER_ID, NotificationStatus.PENDING, null);
		assertThatThrownBy(() -> service.markNotificationRead(new MarkNotificationReadCommand(USER_ID, NOTIFICATION_ID)))
			.isInstanceOf(NotificationMutationRejectedException.class)
			.hasMessage("only delivered notifications can be marked read.");
	}

	@Test
	void markNotificationReadReturnsNotFoundWhenMissing() {
		FakeRepository repository = new FakeRepository();
		repository.notification = null;
		NotificationService service = new NotificationService(repository, CLOCK);

		assertThatThrownBy(() -> service.markNotificationRead(new MarkNotificationReadCommand(USER_ID, NOTIFICATION_ID)))
			.isInstanceOf(NotificationNotFoundException.class)
			.hasMessage("notification was not found.");
	}

	@Test
	void markAllMyNotificationsReadUsesOnlyAuthenticatedRecipient() {
		FakeRepository repository = new FakeRepository();
		NotificationService service = new NotificationService(repository, CLOCK);

		service.markAllMyNotificationsRead(new MarkAllMyNotificationsReadCommand(USER_ID));

		assertThat(repository.markAllRecipientUserId).isEqualTo(USER_ID);
		assertThat(repository.markAllReadAt).isEqualTo(NOW);
	}

	@Test
	void createNotificationStoresDeliveredInAppNotification() {
		FakeRepository repository = new FakeRepository();
		NotificationService service = new NotificationService(repository, CLOCK, () -> NOTIFICATION_ID);

		Notification result = service.createNotification(command(NotificationType.ONBOARDING_REQUESTED, "notification:onboarding"));

		assertThat(result).isSameAs(repository.savedNotification);
		assertThat(result.id()).isEqualTo(NOTIFICATION_ID);
		assertThat(result.status()).isEqualTo(NotificationStatus.DELIVERED);
		assertThat(result.channel()).isEqualTo(NotificationChannel.IN_APP);
		assertThat(result.deliveredAt()).isEqualTo(NOW);
		assertThat(result.createdAt()).isEqualTo(NOW);
	}

	@Test
	void recordNotificationFailureStoresRedactedFailureAndRetryCount() {
		FakeRepository repository = new FakeRepository();
		NotificationService service = new NotificationService(repository, CLOCK, () -> NOTIFICATION_ID);

		Notification result = service.recordNotificationFailure(new RecordNotificationFailureCommand(
			command(NotificationType.WEEK_STARTED, "notification:week-started"),
			"token=plain sk-live-secret api_key=raw\nline2"
		));

		assertThat(result).isSameAs(repository.failedNotification);
		assertThat(result.status()).isEqualTo(NotificationStatus.FAILED);
		assertThat(result.retryCount()).isEqualTo(1);
		assertThat(result.deliveredAt()).isNull();
		assertThat(result.errorMessage())
			.contains("token=[REDACTED]", "api_key=[REDACTED]", "line2")
			.doesNotContain("plain", "sk-live-secret", "raw", "\n");
	}

	@Test
	void retryNotificationRequiresFailedNotificationAndMarksDelivered() {
		FakeRepository repository = new FakeRepository();
		repository.notification = notification(USER_ID, NotificationStatus.DELIVERED, null);
		NotificationService service = new NotificationService(repository, CLOCK);

		assertThatThrownBy(() -> service.retryNotification(new RetryNotificationCommand(NOTIFICATION_ID)))
			.isInstanceOf(NotificationMutationRejectedException.class)
			.hasMessage("only failed notifications can be retried.");

		repository.notification = failedNotification();
		Notification result = service.retryNotification(new RetryNotificationCommand(NOTIFICATION_ID));

		assertThat(repository.retryNotificationId).isEqualTo(NOTIFICATION_ID);
		assertThat(repository.retryDeliveredAt).isEqualTo(NOW);
		assertThat(result.status()).isEqualTo(NotificationStatus.DELIVERED);
		assertThat(result.deliveredAt()).isEqualTo(NOW);
		assertThat(result.errorMessage()).isNull();
	}

	@Test
	void publishWeekStartedCreatesDeliveredNotificationsForActiveRecipients() {
		FakeRepository repository = new FakeRepository();
		repository.activeRecipientUserIds = List.of(USER_ID, OTHER_USER_ID);
		NotificationService service = new NotificationService(repository, CLOCK, () -> NOTIFICATION_ID);

		service.publishWeekStarted(GROUP_ID, WEEK_ID, 1, "JPA 입문");

		assertThat(repository.savedNotifications).hasSize(2);
		assertThat(repository.savedNotifications)
			.extracting(Notification::recipientUserId)
			.containsExactly(USER_ID, OTHER_USER_ID);
		assertThat(repository.savedNotifications)
			.allSatisfy(notification -> {
				assertThat(notification.notificationType()).isEqualTo(NotificationType.WEEK_STARTED);
				assertThat(notification.relatedResources().weekId()).isEqualTo(WEEK_ID);
				assertThat(notification.idempotencyKey()).contains(WEEK_ID.toString(), notification.recipientUserId().toString());
				assertThat(notification.payload()).containsEntry("weekTitle", "JPA 입문");
			});
	}

	@Test
	void createNotificationCommandRejectsBlankIdempotencyKey() {
		assertThatThrownBy(() -> new CreateNotificationCommand(
				GROUP_ID,
				USER_ID,
				null,
				NotificationType.ONBOARDING_REQUESTED,
				" ",
				"알림 제목",
				"알림 본문",
				Map.of(),
				null
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("idempotencyKey must not be blank.");
	}

	@Test
	void listGroupNotificationsRequiresOwnerWhoHasNotLeft() {
		FakeRepository repository = new FakeRepository();
		Notification notification = notification(USER_ID, NotificationStatus.DELIVERED, null);
		repository.groupNotifications = List.of(notification);
		NotificationService service = new NotificationService(repository, CLOCK);

		List<Notification> result = service.listGroupNotifications(new ListGroupNotificationsQuery(USER_ID, GROUP_ID));

		assertThat(result).containsExactly(notification);
		assertThat(repository.requestedGroupId).isEqualTo(GROUP_ID);
		assertThat(repository.requestedLimit).isEqualTo(100);

		repository.accessContext = access(GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
		assertThatThrownBy(() -> service.listGroupNotifications(new ListGroupNotificationsQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(NotificationAccessDeniedException.class)
			.hasMessage("only the study group owner can read notification logs.");

		repository.accessContext = access(GroupMemberPermission.OWNER, GroupMemberStatus.LEFT);
		assertThatThrownBy(() -> service.listGroupNotifications(new ListGroupNotificationsQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(NotificationAccessDeniedException.class)
			.hasMessage("only the study group owner can read notification logs.");
	}

	@Test
	void listGroupNotificationsDistinguishesMissingGroupFromCrossGroupAccess() {
		FakeRepository repository = new FakeRepository();
		repository.accessContext = null;
		repository.groupExists = false;
		NotificationService service = new NotificationService(repository, CLOCK);

		assertThatThrownBy(() -> service.listGroupNotifications(new ListGroupNotificationsQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(NotificationGroupNotFoundException.class)
			.hasMessage("study group was not found.");

		repository.groupExists = true;
		assertThatThrownBy(() -> service.listGroupNotifications(new ListGroupNotificationsQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(NotificationAccessDeniedException.class)
			.hasMessage("authenticated user is not an owner of this study group.");
	}

	private static Notification notification(UUID recipientUserId, NotificationStatus status, Instant readAt) {
		return new Notification(
			NOTIFICATION_ID,
			GROUP_ID,
			recipientUserId,
			new NotificationRelatedResources(null, null, null, null),
			NotificationType.RETROSPECTIVE_READY,
			NotificationChannel.IN_APP,
			"notification:test",
			"회고 피드백이 준비됐어요",
			"AI 팀장 피드백을 확인해 주세요.",
			Map.of("deepLink", "/retrospectives"),
			status,
			status == NotificationStatus.PENDING ? null : NOW.minusSeconds(30),
			readAt,
			null,
			0,
			null,
			NOW.minusSeconds(60)
		);
	}

	private static Notification failedNotification() {
		return new Notification(
			NOTIFICATION_ID,
			GROUP_ID,
			USER_ID,
			new NotificationRelatedResources(null, null, null, null),
			NotificationType.RETROSPECTIVE_READY,
			NotificationChannel.IN_APP,
			"notification:test",
			"회고 피드백이 준비됐어요",
			"AI 팀장 피드백을 확인해 주세요.",
			Map.of("deepLink", "/retrospectives"),
			NotificationStatus.FAILED,
			null,
			null,
			"delivery failed",
			2,
			null,
			NOW.minusSeconds(60)
		);
	}

	private static CreateNotificationCommand command(NotificationType type, String idempotencyKey) {
		return new CreateNotificationCommand(
			GROUP_ID,
			USER_ID,
			new NotificationRelatedResources(null, WEEK_ID, null, null),
			type,
			idempotencyKey,
			"알림 제목",
			"알림 본문",
			Map.of("deepLink", "/test"),
			null
		);
	}

	private static NotificationAccessContext access(GroupMemberPermission permission, GroupMemberStatus memberStatus) {
		return new NotificationAccessContext(GROUP_ID, MEMBER_ID, StudyGroupStatus.ACTIVE, permission, memberStatus);
	}

	private static final class FakeRepository implements NotificationRepository {

		private boolean groupExists = true;
		private NotificationAccessContext accessContext = access(GroupMemberPermission.OWNER, GroupMemberStatus.ACTIVE);
		private Notification notification = notification(USER_ID, NotificationStatus.DELIVERED, null);
		private List<Notification> myNotifications = List.of();
		private List<Notification> groupNotifications = List.of();
		private UUID requestedUserId;
		private boolean requestedUnreadOnly;
		private UUID requestedGroupId;
		private int requestedLimit;
		private UUID updatedNotificationId;
		private UUID updatedRecipientUserId;
		private Instant updatedReadAt;
		private UUID markAllRecipientUserId;
		private Instant markAllReadAt;
		private Notification savedNotification;
		private final List<Notification> savedNotifications = new ArrayList<>();
		private Notification failedNotification;
		private List<UUID> activeRecipientUserIds = List.of();
		private UUID retryNotificationId;
		private Instant retryDeliveredAt;

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			return groupExists;
		}

		@Override
		public Optional<NotificationAccessContext> findAccessContext(UUID groupId, UUID userId) {
			return Optional.ofNullable(accessContext);
		}

		@Override
		public Optional<Notification> findNotification(UUID notificationId) {
			return Optional.ofNullable(notification);
		}

		@Override
		public Optional<Notification> findNotificationByIdempotencyKey(String idempotencyKey) {
			return savedNotifications.stream()
				.filter(candidate -> candidate.idempotencyKey().equals(idempotencyKey))
				.findFirst();
		}

		@Override
		public List<Notification> findMyNotifications(UUID userId, boolean unreadOnly, int limit) {
			requestedUserId = userId;
			requestedUnreadOnly = unreadOnly;
			requestedLimit = limit;
			return myNotifications;
		}

		@Override
		public List<Notification> findGroupNotifications(UUID groupId, int limit) {
			requestedGroupId = groupId;
			requestedLimit = limit;
			return groupNotifications;
		}

		@Override
		public List<UUID> findActiveGroupRecipientUserIds(UUID groupId) {
			return activeRecipientUserIds;
		}

		@Override
		public Notification saveNotification(Notification notification) {
			savedNotification = notification;
			savedNotifications.add(notification);
			return notification;
		}

		@Override
		public Notification recordFailedNotification(Notification notification) {
			failedNotification = notification;
			return notification;
		}

		@Override
		public Notification retryFailedNotification(UUID notificationId, Instant deliveredAt) {
			retryNotificationId = notificationId;
			retryDeliveredAt = deliveredAt;
			notification = notification.retryDelivered(deliveredAt);
			return notification;
		}

		@Override
		public boolean markNotificationRead(UUID notificationId, UUID recipientUserId, Instant readAt) {
			updatedNotificationId = notificationId;
			updatedRecipientUserId = recipientUserId;
			updatedReadAt = readAt;
			return true;
		}

		@Override
		public int markAllDeliveredNotificationsRead(UUID recipientUserId, Instant readAt) {
			markAllRecipientUserId = recipientUserId;
			markAllReadAt = readAt;
			return 3;
		}
	}
}
