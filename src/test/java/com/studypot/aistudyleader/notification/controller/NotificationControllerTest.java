package com.studypot.aistudyleader.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.notification.domain.Notification;
import com.studypot.aistudyleader.notification.domain.NotificationAccessContext;
import com.studypot.aistudyleader.notification.domain.NotificationChannel;
import com.studypot.aistudyleader.notification.domain.NotificationRelatedResources;
import com.studypot.aistudyleader.notification.domain.NotificationStatus;
import com.studypot.aistudyleader.notification.domain.NotificationType;
import com.studypot.aistudyleader.notification.repository.NotificationRepository;
import com.studypot.aistudyleader.notification.service.NotificationService;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(classes = {AiStudyLeaderApplication.class, NotificationControllerTest.TestNotificationBeans.class})
@AutoConfigureMockMvc
class NotificationControllerTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000008301");
	private static final UUID OTHER_USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000008302");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000008303");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000008304");
	private static final UUID NOTIFICATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000008305");
	private static final Instant NOW = Instant.parse("2026-05-13T06:00:00Z");
	private static final String MY_NOTIFICATIONS_PATH = ApiPaths.V1 + "/users/me/notifications";
	private static final String READ_PATH = ApiPaths.V1 + "/notifications/" + NOTIFICATION_ID + "/read";
	private static final String READ_ALL_PATH = ApiPaths.V1 + "/users/me/notifications/read-all";
	private static final String GROUP_NOTIFICATIONS_PATH = ApiPaths.V1 + "/groups/" + GROUP_ID + "/notifications";

	private final MockMvc mockMvc;
	private final MutableNotificationRepository repository;

	@Autowired
	NotificationControllerTest(MockMvc mockMvc, MutableNotificationRepository repository) {
		this.mockMvc = mockMvc;
		this.repository = repository;
	}

	@BeforeEach
	void setUp() {
		repository.reset();
	}

	@Test
	void listMyNotificationsRequiresAuthentication() throws Exception {
		mockMvc.perform(get(MY_NOTIFICATIONS_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void listMyNotificationsReturnsOnlyAuthenticatedUserNotifications() throws Exception {
		mockMvc.perform(get(MY_NOTIFICATIONS_PATH)
				.param("unreadOnly", "true")
				.with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$[0].id").value(NOTIFICATION_ID.toString()))
			.andExpect(jsonPath("$[0].notificationType").value("RETROSPECTIVE_READY"))
			.andExpect(jsonPath("$[0].channel").value("IN_APP"))
			.andExpect(jsonPath("$[0].title").value("회고 피드백이 준비됐어요"))
			.andExpect(jsonPath("$[0].body").value("AI 팀장 피드백을 확인해 주세요."))
			.andExpect(jsonPath("$[0].status").value("DELIVERED"))
			.andExpect(jsonPath("$[0].deliveredAt").value("2026-05-13T05:59:30Z"));

		assertThat(repository.lastUnreadOnly).isTrue();
	}

	@Test
	void markNotificationReadReturnsUpdatedNotification() throws Exception {
		mockMvc.perform(post(READ_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("notification-xsrf")))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(NOTIFICATION_ID.toString()))
			.andExpect(jsonPath("$.status").value("READ"))
			.andExpect(jsonPath("$.readAt").value("2026-05-13T06:00:00Z"));
	}

	@Test
	void markNotificationReadReturnsForbiddenForOtherRecipient() throws Exception {
		repository.notification = notification(OTHER_USER_ID, NotificationStatus.DELIVERED, null);

		mockMvc.perform(post(READ_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("notification-xsrf")))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Forbidden"));
	}

	@Test
	void markAllMyNotificationsReadReturnsNoContent() throws Exception {
		mockMvc.perform(post(READ_ALL_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("notification-xsrf")))
			.andExpect(status().isNoContent())
			.andExpect(content().string(""));

		assertThat(repository.markAllRecipientUserId).isEqualTo(USER_ID);
	}

	@Test
	void listGroupNotificationsReturnsOwnerAuditLogs() throws Exception {
		mockMvc.perform(get(GROUP_NOTIFICATIONS_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$[0].id").value(NOTIFICATION_ID.toString()))
			.andExpect(jsonPath("$[0].status").value("DELIVERED"));
	}

	@Test
	void listGroupNotificationsReturnsForbiddenForNonOwner() throws Exception {
		repository.accessContext = access(GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);

		mockMvc.perform(get(GROUP_NOTIFICATIONS_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Forbidden"));
	}

	@Test
	void listGroupNotificationsReturnsNotFoundForMissingGroup() throws Exception {
		repository.accessContext = null;
		repository.groupExists = false;

		mockMvc.perform(get(GROUP_NOTIFICATIONS_PATH)
				.with(user(USER_ID.toString())))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestNotificationBeans {

		@Bean
		MutableNotificationRepository mutableNotificationRepository() {
			return new MutableNotificationRepository();
		}

		@Bean
		@Primary
		NotificationService testNotificationService(MutableNotificationRepository repository) {
			return new NotificationService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
		}
	}

	static final class MutableNotificationRepository implements NotificationRepository {

		private boolean groupExists;
		private NotificationAccessContext accessContext;
		private Notification notification;
		private List<Notification> notifications;
		private boolean lastUnreadOnly;
		private UUID markAllRecipientUserId;

		void reset() {
			groupExists = true;
			accessContext = access(GroupMemberPermission.OWNER, GroupMemberStatus.ACTIVE);
			notification = notification(USER_ID, NotificationStatus.DELIVERED, null);
			notifications = List.of(notification);
			lastUnreadOnly = false;
			markAllRecipientUserId = null;
		}

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
			return notifications.stream()
				.filter(candidate -> candidate.idempotencyKey().equals(idempotencyKey))
				.findFirst();
		}

		@Override
		public List<Notification> findMyNotifications(UUID userId, boolean unreadOnly, int limit) {
			lastUnreadOnly = unreadOnly;
			return notifications.stream()
				.filter(candidate -> candidate.recipientUserId().equals(userId))
				.toList();
		}

		@Override
		public List<Notification> findGroupNotifications(UUID groupId, int limit) {
			return notifications.stream()
				.filter(candidate -> candidate.groupId().equals(groupId))
				.toList();
		}

		@Override
		public List<UUID> findActiveGroupRecipientUserIds(UUID groupId) {
			return notifications.stream()
				.map(Notification::recipientUserId)
				.distinct()
				.toList();
		}

		@Override
		public Notification saveNotification(Notification notification) {
			notifications = List.of(notification);
			this.notification = notification;
			return notification;
		}

		@Override
		public Notification recordFailedNotification(Notification notification) {
			notifications = List.of(notification);
			this.notification = notification;
			return notification;
		}

		@Override
		public Notification retryFailedNotification(UUID notificationId, Instant deliveredAt) {
			notification = notification.retryDelivered(deliveredAt);
			notifications = List.of(notification);
			return notification;
		}

		@Override
		public boolean markNotificationRead(UUID notificationId, UUID recipientUserId, Instant readAt) {
			notification = notification.markRead(readAt);
			notifications = List.of(notification);
			return true;
		}

		@Override
		public int markAllDeliveredNotificationsRead(UUID recipientUserId, Instant readAt) {
			markAllRecipientUserId = recipientUserId;
			return 1;
		}
	}

	private static NotificationAccessContext access(GroupMemberPermission permission, GroupMemberStatus memberStatus) {
		return new NotificationAccessContext(GROUP_ID, MEMBER_ID, StudyGroupStatus.ACTIVE, permission, memberStatus);
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

	private static RequestPostProcessor xsrf(String value) {
		return request -> {
			jakarta.servlet.http.Cookie[] existingCookies = request.getCookies();
			jakarta.servlet.http.Cookie[] cookies = existingCookies == null
				? new jakarta.servlet.http.Cookie[1]
				: java.util.Arrays.copyOf(existingCookies, existingCookies.length + 1);
			cookies[cookies.length - 1] = new MockCookie("XSRF-TOKEN", value);
			request.setCookies(cookies);
			request.addHeader("X-XSRF-TOKEN", value);
			return request;
		};
	}
}
