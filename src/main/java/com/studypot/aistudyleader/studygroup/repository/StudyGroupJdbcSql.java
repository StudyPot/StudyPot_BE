package com.studypot.aistudyleader.studygroup.repository;

final class StudyGroupJdbcSql {

	static final String INSERT_STUDY_GROUP = """
		insert into study_group (
		  id, created_by, name, description, topic, detail_keywords, status, max_members,
		  is_public, invite_code, starts_at, ends_at, onboarding_started_at, started_at,
		  created_at, updated_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String INSERT_GROUP_MEMBER = """
		insert into group_member (
		  id, group_id, user_id, permission, status, display_name, joined_at,
		  activated_at, left_at, created_at, updated_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String SELECT_STUDY_GROUP_JOIN_TARGET = """
		select id, status, max_members, invite_code
		from study_group
		where id = ?
		  and deleted_at is null
		for update
		""";

	static final String EXISTS_STUDY_GROUP = """
		select exists (
		  select 1
		  from study_group
		  where id = ?
		    and deleted_at is null
		)
		""";

	static final String SELECT_GROUP_BY_ID_FOR_MEMBER_USER_ID = """
		select
		  sg.id, sg.created_by, sg.name, sg.description, sg.topic, sg.detail_keywords,
		  sg.status, sg.max_members, sg.is_public, sg.invite_code, sg.starts_at, sg.ends_at,
		  sg.onboarding_started_at, sg.started_at, sg.created_at, sg.updated_at
		from study_group sg
		join group_member gm on gm.group_id = sg.id
		where sg.id = ?
		  and gm.user_id = ?
		  and gm.status in ('PENDING_ONBOARDING', 'ACTIVE')
		  and gm.deleted_at is null
		  and sg.status in ('ONBOARDING', 'READY_TO_START', 'ACTIVE', 'COMPLETED')
		  and sg.deleted_at is null
		""";

	static final String EXISTS_ACTIVE_OR_ONBOARDING_MEMBER = """
		select exists (
		  select 1
		  from group_member
		  where group_id = ?
		    and user_id = ?
		    and status in ('PENDING_ONBOARDING', 'ACTIVE')
		    and deleted_at is null
		)
		""";

	static final String COUNT_ACTIVE_OR_ONBOARDING_MEMBERS = """
		select count(*)
		from group_member
		where group_id = ?
		  and status in ('PENDING_ONBOARDING', 'ACTIVE')
		  and deleted_at is null
		""";

	static final String SELECT_GROUPS_BY_MEMBER_USER_ID = """
		select
		  sg.id, sg.created_by, sg.name, sg.description, sg.topic, sg.detail_keywords,
		  sg.status, sg.max_members, sg.is_public, sg.invite_code, sg.starts_at, sg.ends_at,
		  sg.onboarding_started_at, sg.started_at, sg.created_at, sg.updated_at
		from study_group sg
		join group_member gm on gm.group_id = sg.id
		where gm.user_id = ?
		  and gm.status in ('PENDING_ONBOARDING', 'ACTIVE')
		  and gm.deleted_at is null
		  and sg.status in ('ONBOARDING', 'READY_TO_START', 'ACTIVE', 'COMPLETED')
		  and sg.deleted_at is null
		order by gm.joined_at desc, sg.created_at desc, sg.id desc
		""";

	static final String SELECT_MY_GROUP_MEMBER_PROFILE = """
		select
		  gm.group_id, gm.id as member_id, gm.user_id, gm.display_name,
		  gm.permission, gm.status as member_status,
		  gor.status as onboarding_status, gor.keyword_skill_levels,
		  gor.submitted_at as onboarding_submitted_at,
		  (
		    select count(*)
		    from member_availability_slot mas
		    where mas.onboarding_response_id = gor.id
		      and mas.deleted_at is null
		  ) as availability_slot_count,
		  cw.id as current_week_id, cw.week_number, cw.sprint_goal,
		  cw.starts_at as week_starts_at, cw.ends_at as week_ends_at,
		  mwp.status as progress_status,
		  (
		    select count(*)
		    from weekly_task wt
		    where wt.curriculum_week_id = cw.id
		      and wt.deleted_at is null
		  ) as task_total_count,
		  (
		    select count(*)
		    from weekly_task wt
		    join task_completion tc on tc.weekly_task_id = wt.id
		    where wt.curriculum_week_id = cw.id
		      and wt.deleted_at is null
		      and tc.member_id = gm.id
		      and tc.status = 'DONE'
		  ) as task_done_count,
		  (
		    select count(*)
		    from weekly_task wt
		    join task_completion tc on tc.weekly_task_id = wt.id
		    where wt.curriculum_week_id = cw.id
		      and wt.deleted_at is null
		      and tc.member_id = gm.id
		      and tc.status = 'INCOMPLETE'
		  ) as task_incomplete_count,
		  (
		    select count(*)
		    from weekly_task wt
		    join task_completion tc on tc.weekly_task_id = wt.id
		    where wt.curriculum_week_id = cw.id
		      and wt.deleted_at is null
		      and tc.member_id = gm.id
		      and tc.status = 'SKIPPED'
		  ) as task_skipped_count,
		  exists (
		    select 1
		    from retrospective r
		    where r.member_id = gm.id
		      and r.progress_id = mwp.id
		      and r.status = 'COMPLETED'
		  ) as retrospective_feedback_ready
		from study_group sg
		join group_member gm on gm.group_id = sg.id
		left join group_onboarding_response gor on gor.group_id = sg.id
		  and gor.member_id = gm.id
		  and gor.status = 'SUBMITTED'
		  and gor.deleted_at is null
		left join curriculum c on c.group_id = sg.id
		  and c.status = 'ACTIVE'
		  and c.deleted_at is null
		left join curriculum_week cw on cw.curriculum_id = c.id
		  and cw.status = 'IN_PROGRESS'
		  and cw.deleted_at is null
		left join member_week_progress mwp on mwp.curriculum_week_id = cw.id
		  and mwp.member_id = gm.id
		where sg.id = ?
		  and gm.user_id = ?
		  and gm.status in ('PENDING_ONBOARDING', 'ACTIVE')
		  and gm.deleted_at is null
		  and sg.status in ('ONBOARDING', 'READY_TO_START', 'ACTIVE', 'COMPLETED')
		  and sg.deleted_at is null
		order by cw.week_number
		limit 1
		""";

	static final String UPDATE_MY_GROUP_MEMBER_DISPLAY_NAME = """
		update group_member gm
		join study_group sg on sg.id = gm.group_id
		set gm.display_name = ?,
		    gm.updated_at = ?
		where gm.group_id = ?
		  and gm.user_id = ?
		  and gm.status in ('PENDING_ONBOARDING', 'ACTIVE')
		  and gm.deleted_at is null
		  and sg.deleted_at is null
		""";

	static final String SOFT_DELETE_GROUP = """
		update study_group
		set deleted_at = ?,
		    updated_at = ?
		where id = ?
		  and deleted_at is null
		""";

	static final String SELECT_GROUP_MEMBERS = """
		select
		  gm.id as member_id, gm.group_id, gm.user_id, gm.display_name,
		  gm.permission, gm.status as member_status,
		  u.nickname, u.email,
		  gor.status as onboarding_status
		from group_member gm
		join users u on u.id = gm.user_id
		left join group_onboarding_response gor
		  on gor.member_id = gm.id
		  and gor.deleted_at is null
		where gm.group_id = ?
		  and gm.deleted_at is null
		order by gm.joined_at asc, gm.id asc
		""";

	private StudyGroupJdbcSql() {
	}
}
