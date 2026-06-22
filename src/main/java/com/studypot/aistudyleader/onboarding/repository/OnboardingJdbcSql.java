package com.studypot.aistudyleader.onboarding.repository;

final class OnboardingJdbcSql {

	static final String EXISTS_STUDY_GROUP = """
		select exists (
		  select 1
		  from study_group
		  where id = ?
		    and deleted_at is null
		)
		""";

	static final String SELECT_MEMBER_CONTEXT = """
		select sg.id as group_id, gm.id as member_id, gm.status as member_status, sg.detail_keywords
		from study_group sg
		join group_member gm on gm.group_id = sg.id
		where sg.id = ?
		  and gm.user_id = ?
		  and gm.status in ('PENDING_ONBOARDING', 'ACTIVE')
		  and sg.deleted_at is null
		  and gm.deleted_at is null
		""";

	static final String SELECT_RESPONSE_BY_MEMBER = """
		select id, group_id, member_id, keyword_skill_levels, task_preferences,
		       additional_note, status, submitted_at, created_at, updated_at
		from group_onboarding_response
		where member_id = ?
		  and deleted_at is null
		""";

	static final String SELECT_AVAILABILITY_SLOTS_BY_RESPONSE = """
		select id, onboarding_response_id, member_id, day_of_week, start_time,
		       end_time, timezone, created_at, updated_at
		from member_availability_slot
		where onboarding_response_id = ?
		  and deleted_at is null
		order by day_of_week, start_time, end_time
		""";

	static final String UPSERT_ONBOARDING_RESPONSE_DRAFT = """
		insert into group_onboarding_response (
		  id, group_id, member_id, keyword_skill_levels, task_preferences,
		  additional_note, status, submitted_at, created_at, updated_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		on duplicate key update
		  keyword_skill_levels = values(keyword_skill_levels),
		  task_preferences = values(task_preferences),
		  additional_note = values(additional_note),
		  status = values(status),
		  submitted_at = null,
		  updated_at = values(updated_at)
		""";

	static final String SUBMIT_ONBOARDING_RESPONSE = """
		update group_onboarding_response
		set status = 'SUBMITTED',
		    submitted_at = ?,
		    updated_at = ?
		where id = ?
		  and member_id = ?
		  and deleted_at is null
		""";

	static final String ACTIVATE_PENDING_MEMBER = """
		update group_member
		set status = 'ACTIVE',
		    activated_at = ?,
		    updated_at = ?
		where id = ?
		  and status = 'PENDING_ONBOARDING'
		  and deleted_at is null
		""";

	static final String MARK_STUDY_GROUP_READY_TO_START = """
		update study_group sg
		set sg.status = 'READY_TO_START',
		    sg.updated_at = ?
		where sg.id = ?
		  and sg.status = 'ONBOARDING'
		  and sg.deleted_at is null
		""";

	static final String SOFT_DELETE_AVAILABILITY_SLOTS_BY_RESPONSE = """
		update member_availability_slot
		set deleted_at = current_timestamp(6),
		    updated_at = current_timestamp(6)
		where onboarding_response_id = ?
		  and deleted_at is null
		""";

	static final String INSERT_AVAILABILITY_SLOT = """
		insert into member_availability_slot (
		  id, onboarding_response_id, member_id, day_of_week, start_time,
		  end_time, timezone, created_at, updated_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String SELECT_RESPONSES_BY_GROUP = """
		select gor.id, gm.group_id, gm.id as member_id, gor.keyword_skill_levels, gor.task_preferences,
		       gor.additional_note, gor.status, gor.submitted_at, gor.created_at, gor.updated_at,
		       u.nickname as member_nickname, gm.status as member_status
		from group_member gm
		join users u on u.id = gm.user_id
		left join group_onboarding_response gor
		  on gor.member_id = gm.id and gor.deleted_at is null
		where gm.group_id = ?
		  and gm.deleted_at is null
		  and gm.status in ('PENDING_ONBOARDING', 'ACTIVE')
		order by gm.joined_at asc, gm.id asc
		""";

	static final String SELECT_OWNER_USER_ID_WHEN_ALL_ONBOARDED = """
		select owner.user_id
		from group_member owner
		join study_group sg on sg.id = owner.group_id
		where owner.group_id = ?
		  and owner.permission = 'OWNER'
		  and owner.status = 'ACTIVE'
		  and owner.deleted_at is null
		  and sg.deleted_at is null
		  and not exists (
		    select 1
		    from group_member gm
		    left join group_onboarding_response gor
		      on gor.member_id = gm.id and gor.deleted_at is null
		    where gm.group_id = owner.group_id
		      and gm.status in ('PENDING_ONBOARDING', 'ACTIVE')
		      and gm.deleted_at is null
		      and (gor.id is null or gor.status <> 'SUBMITTED')
		  )
		""";

	private OnboardingJdbcSql() {
	}
}
