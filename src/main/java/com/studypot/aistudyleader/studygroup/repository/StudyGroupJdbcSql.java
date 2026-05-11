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
		  and sg.status in ('ONBOARDING', 'ACTIVE', 'COMPLETED')
		  and sg.deleted_at is null
		order by gm.joined_at desc, sg.created_at desc, sg.id desc
		""";

	private StudyGroupJdbcSql() {
	}
}
