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

	static final String EXISTS_ACTIVE_GROUP_BY_INVITE_CODE = """
		select exists(
		  select 1
		  from study_group
		  where invite_code = ?
		    and deleted_at is null
		)
		""";

	private StudyGroupJdbcSql() {
	}
}
