package com.studypot.aistudyleader.notification.repository;

final class NotificationJdbcSql {

	static final String EXISTS_STUDY_GROUP = """
		select exists (
		  select 1
		  from study_group
		  where id = ?
		    and deleted_at is null
		)
		""";

	static final String SELECT_ACCESS_CONTEXT = """
		select sg.id as group_id, gm.id as member_id, sg.status as group_status,
		       gm.permission, gm.status as member_status
		from study_group sg
		join group_member gm on gm.group_id = sg.id
		where sg.id = ?
		  and gm.user_id = ?
		  and sg.deleted_at is null
		  and gm.deleted_at is null
		""";

	static final String SELECT_NOTIFICATION_BY_ID = """
		select id, group_id, recipient_user_id, related_onboarding_response_id, related_week_id,
		       related_task_completion_id, related_retrospective_id, notification_type, channel,
		       idempotency_key, title, body, payload, status, delivered_at, read_at, error_message,
		       retry_count, scheduled_at, created_at
		from notification
		where id = ?
		""";

	static final String SELECT_NOTIFICATION_BY_IDEMPOTENCY_KEY = """
		select id, group_id, recipient_user_id, related_onboarding_response_id, related_week_id,
		       related_task_completion_id, related_retrospective_id, notification_type, channel,
		       idempotency_key, title, body, payload, status, delivered_at, read_at, error_message,
		       retry_count, scheduled_at, created_at
		from notification
		where idempotency_key = ?
		""";

	static final String SELECT_MY_NOTIFICATIONS = """
		select id, group_id, recipient_user_id, related_onboarding_response_id, related_week_id,
		       related_task_completion_id, related_retrospective_id, notification_type, channel,
		       idempotency_key, title, body, payload, status, delivered_at, read_at, error_message,
		       retry_count, scheduled_at, created_at
		from notification
		where recipient_user_id = ?
		order by created_at desc, id desc
		limit ?
		""";

	static final String SELECT_MY_UNREAD_NOTIFICATIONS = """
		select id, group_id, recipient_user_id, related_onboarding_response_id, related_week_id,
		       related_task_completion_id, related_retrospective_id, notification_type, channel,
		       idempotency_key, title, body, payload, status, delivered_at, read_at, error_message,
		       retry_count, scheduled_at, created_at
		from notification
		where recipient_user_id = ?
		  and read_at is null
		  and status <> 'READ'
		order by created_at desc, id desc
		limit ?
		""";

	static final String SELECT_GROUP_NOTIFICATIONS = """
		select n.id, n.group_id, n.recipient_user_id, n.related_onboarding_response_id, n.related_week_id,
		       n.related_task_completion_id, n.related_retrospective_id, n.notification_type, n.channel,
		       n.idempotency_key, n.title, n.body, n.payload, n.status, n.delivered_at, n.read_at, n.error_message,
		       n.retry_count, n.scheduled_at, n.created_at
		from notification n
		join study_group sg on sg.id = n.group_id
		where n.group_id = ?
		  and sg.deleted_at is null
		order by n.created_at desc, n.id desc
		limit ?
		""";

	static final String SELECT_ACTIVE_GROUP_RECIPIENT_USER_IDS = """
		select gm.user_id
		from group_member gm
		join study_group sg on sg.id = gm.group_id
		where gm.group_id = ?
		  and gm.status = 'ACTIVE'
		  and gm.deleted_at is null
		  and sg.deleted_at is null
		order by gm.joined_at asc, gm.id asc
		""";

	static final String INSERT_NOTIFICATION = """
		insert into notification (
		  id, group_id, recipient_user_id, related_onboarding_response_id, related_week_id,
		  related_task_completion_id, related_retrospective_id, notification_type, channel,
		  idempotency_key, title, body, payload, status, delivered_at, read_at, error_message,
		  retry_count, scheduled_at, created_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String MARK_NOTIFICATION_FAILED_BY_IDEMPOTENCY_KEY = """
		update notification
		set status = 'FAILED',
		    delivered_at = null,
		    read_at = null,
		    error_message = ?,
		    retry_count = retry_count + 1
		where idempotency_key = ?
		  and status in ('PENDING', 'FAILED')
		""";

	static final String RETRY_FAILED_NOTIFICATION = """
		update notification
		set status = 'DELIVERED',
		    delivered_at = ?,
		    error_message = null
		where id = ?
		  and status = 'FAILED'
		""";

	static final String MARK_NOTIFICATION_READ = """
		update notification
		set status = 'READ',
		    read_at = ?
		where id = ?
		  and recipient_user_id = ?
		  and status = 'DELIVERED'
		  and read_at is null
		""";

	static final String MARK_ALL_DELIVERED_NOTIFICATIONS_READ = """
		update notification
		set status = 'READ',
		    read_at = ?
		where recipient_user_id = ?
		  and status = 'DELIVERED'
		  and read_at is null
		""";

	private NotificationJdbcSql() {
	}
}
