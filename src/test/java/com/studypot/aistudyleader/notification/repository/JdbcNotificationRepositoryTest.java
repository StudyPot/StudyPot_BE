package com.studypot.aistudyleader.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.notification.domain.Notification;
import com.studypot.aistudyleader.notification.domain.NotificationAccessContext;
import com.studypot.aistudyleader.notification.domain.NotificationChannel;
import com.studypot.aistudyleader.notification.domain.NotificationStatus;
import com.studypot.aistudyleader.notification.domain.NotificationType;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class JdbcNotificationRepositoryTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000008201");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000008202");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000008203");
	private static final UUID NOTIFICATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000008204");
	private static final UUID RETROSPECTIVE_ID = UUID.fromString("018f0000-0000-7000-8000-000000008205");
	private static final Instant NOW = Instant.parse("2026-05-13T05:40:00Z");

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final JdbcNotificationRepository repository = new JdbcNotificationRepository(jdbcTemplate, new ObjectMapper());

	@Test
	void notificationSqlUsesRecipientAndOwnerBoundaries() {
		assertThat(NotificationJdbcSql.SELECT_MY_NOTIFICATIONS)
			.contains("where recipient_user_id = ?")
			.contains("order by created_at desc, id desc")
			.contains("limit ?");
		assertThat(NotificationJdbcSql.SELECT_MY_UNREAD_NOTIFICATIONS)
			.contains("where recipient_user_id = ?")
			.contains("read_at is null")
			.contains("status <> 'READ'");
		assertThat(NotificationJdbcSql.SELECT_ACCESS_CONTEXT)
			.contains("join group_member gm on gm.group_id = sg.id")
			.contains("where sg.id = ?")
			.contains("and gm.user_id = ?")
			.contains("gm.deleted_at is null");
		assertThat(NotificationJdbcSql.SELECT_GROUP_NOTIFICATIONS)
			.contains("join study_group sg on sg.id = n.group_id")
			.contains("where n.group_id = ?")
			.contains("sg.deleted_at is null");
		assertThat(NotificationJdbcSql.MARK_NOTIFICATION_READ)
			.contains("recipient_user_id = ?")
			.contains("status = 'DELIVERED'")
			.contains("read_at is null");
	}

	@Test
	void findNotificationMapsJsonPayloadAndRelatedResources() throws Exception {
		when(jdbcTemplate.query(eq(NotificationJdbcSql.SELECT_NOTIFICATION_BY_ID), any(RowMapper.class), any(Object[].class)))
			.thenAnswer(invocation -> {
				@SuppressWarnings("unchecked")
				RowMapper<Notification> mapper = invocation.getArgument(1);
				return List.of(mapper.mapRow(notificationRow(NotificationStatus.DELIVERED, null), 0));
			});

		Optional<Notification> result = repository.findNotification(NOTIFICATION_ID);

		assertThat(result).isPresent();
		Notification notification = result.orElseThrow();
		assertThat(notification.id()).isEqualTo(NOTIFICATION_ID);
		assertThat(notification.groupId()).isEqualTo(GROUP_ID);
		assertThat(notification.recipientUserId()).isEqualTo(USER_ID);
		assertThat(notification.notificationType()).isEqualTo(NotificationType.RETROSPECTIVE_READY);
		assertThat(notification.channel()).isEqualTo(NotificationChannel.IN_APP);
		assertThat(notification.status()).isEqualTo(NotificationStatus.DELIVERED);
		assertThat(notification.relatedResources().retrospectiveId()).isEqualTo(RETROSPECTIVE_ID);
		assertThat(notification.payload()).containsEntry("deepLink", "/retrospectives");

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(NotificationJdbcSql.SELECT_NOTIFICATION_BY_ID), any(RowMapper.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(NOTIFICATION_ID));
	}

	@Test
	void findAccessContextQueriesGroupAndUser() {
		NotificationAccessContext context = new NotificationAccessContext(
			GROUP_ID,
			MEMBER_ID,
			StudyGroupStatus.ACTIVE,
			GroupMemberPermission.OWNER,
			GroupMemberStatus.ACTIVE
		);
		when(jdbcTemplate.query(eq(NotificationJdbcSql.SELECT_ACCESS_CONTEXT), any(RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(context));

		Optional<NotificationAccessContext> result = repository.findAccessContext(GROUP_ID, USER_ID);

		assertThat(result).contains(context);
		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(NotificationJdbcSql.SELECT_ACCESS_CONTEXT), any(RowMapper.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(USER_ID));
	}

	@Test
	void listQueriesUseStableOrderingAndBoundedLimit() {
		Notification notification = notification(NotificationStatus.DELIVERED, null);
		when(jdbcTemplate.query(eq(NotificationJdbcSql.SELECT_MY_NOTIFICATIONS), any(RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(notification));
		when(jdbcTemplate.query(eq(NotificationJdbcSql.SELECT_MY_UNREAD_NOTIFICATIONS), any(RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(notification));
		when(jdbcTemplate.query(eq(NotificationJdbcSql.SELECT_GROUP_NOTIFICATIONS), any(RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(notification));

		assertThat(repository.findMyNotifications(USER_ID, false, 50)).containsExactly(notification);
		assertThat(repository.findMyNotifications(USER_ID, true, 50)).containsExactly(notification);
		assertThat(repository.findGroupNotifications(GROUP_ID, 50)).containsExactly(notification);
	}

	@Test
	void listQueriesClampCallerProvidedLimits() {
		Notification notification = notification(NotificationStatus.DELIVERED, null);
		when(jdbcTemplate.query(eq(NotificationJdbcSql.SELECT_MY_NOTIFICATIONS), any(RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(notification));
		when(jdbcTemplate.query(eq(NotificationJdbcSql.SELECT_GROUP_NOTIFICATIONS), any(RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(notification));

		assertThat(repository.findMyNotifications(USER_ID, false, 5000)).containsExactly(notification);
		assertThat(repository.findGroupNotifications(GROUP_ID, 0)).containsExactly(notification);

		ArgumentCaptor<Object[]> myArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(NotificationJdbcSql.SELECT_MY_NOTIFICATIONS), any(RowMapper.class), myArgs.capture());
		assertThat(myArgs.getValue()[1]).isEqualTo(1000);

		ArgumentCaptor<Object[]> groupArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(NotificationJdbcSql.SELECT_GROUP_NOTIFICATIONS), any(RowMapper.class), groupArgs.capture());
		assertThat(groupArgs.getValue()[1]).isEqualTo(1);
	}

	@Test
	void markReadUpdatesOnlyRecipientDeliveredRows() {
		when(jdbcTemplate.update(eq(NotificationJdbcSql.MARK_NOTIFICATION_READ), any(Object[].class))).thenReturn(1);

		assertThat(repository.markNotificationRead(NOTIFICATION_ID, USER_ID, NOW)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(NotificationJdbcSql.MARK_NOTIFICATION_READ), args.capture());
		assertThat(args.getValue()[0]).isEqualTo(Timestamp.from(NOW));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(NOTIFICATION_ID));
		assertThat((byte[]) args.getValue()[2]).containsExactly(UuidBinary.toBytes(USER_ID));
	}

	@Test
	void markAllReadUpdatesOnlyAuthenticatedRecipientDeliveredRows() {
		when(jdbcTemplate.update(eq(NotificationJdbcSql.MARK_ALL_DELIVERED_NOTIFICATIONS_READ), any(Object[].class))).thenReturn(3);

		assertThat(repository.markAllDeliveredNotificationsRead(USER_ID, NOW)).isEqualTo(3);

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(NotificationJdbcSql.MARK_ALL_DELIVERED_NOTIFICATIONS_READ), args.capture());
		assertThat(args.getValue()[0]).isEqualTo(Timestamp.from(NOW));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(USER_ID));
	}

	private static ResultSet notificationRow(NotificationStatus status, Instant readAt) throws Exception {
		ResultSet resultSet = mock(ResultSet.class);
		when(resultSet.getBytes("id")).thenReturn(UuidBinary.toBytes(NOTIFICATION_ID));
		when(resultSet.getBytes("group_id")).thenReturn(UuidBinary.toBytes(GROUP_ID));
		when(resultSet.getBytes("recipient_user_id")).thenReturn(UuidBinary.toBytes(USER_ID));
		when(resultSet.getBytes("related_onboarding_response_id")).thenReturn(null);
		when(resultSet.getBytes("related_week_id")).thenReturn(null);
		when(resultSet.getBytes("related_task_completion_id")).thenReturn(null);
		when(resultSet.getBytes("related_retrospective_id")).thenReturn(UuidBinary.toBytes(RETROSPECTIVE_ID));
		when(resultSet.getString("notification_type")).thenReturn("RETROSPECTIVE_READY");
		when(resultSet.getString("channel")).thenReturn("IN_APP");
		when(resultSet.getString("idempotency_key")).thenReturn("retro-ready:" + RETROSPECTIVE_ID);
		when(resultSet.getString("title")).thenReturn("회고 피드백이 준비됐어요");
		when(resultSet.getString("body")).thenReturn("AI 팀장 피드백을 확인해 주세요.");
		when(resultSet.getString("payload")).thenReturn("{\"deepLink\":\"/retrospectives\"}");
		when(resultSet.getString("status")).thenReturn(status.name());
		when(resultSet.getTimestamp("delivered_at")).thenReturn(status == NotificationStatus.PENDING ? null : Timestamp.from(NOW.minusSeconds(30)));
		when(resultSet.getTimestamp("read_at")).thenReturn(readAt == null ? null : Timestamp.from(readAt));
		when(resultSet.getString("error_message")).thenReturn(null);
		when(resultSet.getInt("retry_count")).thenReturn(0);
		when(resultSet.getTimestamp("scheduled_at")).thenReturn(null);
		when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(NOW.minusSeconds(60)));
		return resultSet;
	}

	private static Notification notification(NotificationStatus status, Instant readAt) {
		return new Notification(
			NOTIFICATION_ID,
			GROUP_ID,
			USER_ID,
			null,
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
}
