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

	private StudyGroupJdbcSql() {
	}
}
