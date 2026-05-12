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

	private OnboardingJdbcSql() {
	}
}
