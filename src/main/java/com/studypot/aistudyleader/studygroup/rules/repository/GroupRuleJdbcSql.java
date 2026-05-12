package com.studypot.aistudyleader.studygroup.rules.repository;

final class GroupRuleJdbcSql {

	static final String EXISTS_STUDY_GROUP = """
		select exists (
		  select 1
		  from study_group
		  where id = ?
		    and deleted_at is null
		)
		""";

	static final String SELECT_MEMBERSHIP = """
		select gm.group_id, gm.id as member_id, gm.permission, gm.status as member_status
		from group_member gm
		join study_group sg on sg.id = gm.group_id
		where gm.group_id = ?
		  and gm.user_id = ?
		  and sg.deleted_at is null
		  and gm.deleted_at is null
		""";

	static final String SELECT_MEMBER_BY_ID = """
		select gm.group_id, gm.id as member_id, gm.permission, gm.status as member_status
		from group_member gm
		join study_group sg on sg.id = gm.group_id
		where gm.group_id = ?
		  and gm.id = ?
		  and sg.deleted_at is null
		  and gm.deleted_at is null
		""";

	static final String SELECT_RULE_BY_GROUP_TYPE_FOR_UPDATE = """
		select id, group_id, created_by, rule_type, config, description, is_active,
		       created_at, updated_at, deleted_at
		from group_rule
		where group_id = ?
		  and rule_type = ?
		  and deleted_at is null
		order by created_at desc, id desc
		limit 1
		for update
		""";

	static final String SELECT_RULE_BY_ID = """
		select id, group_id, created_by, rule_type, config, description, is_active,
		       created_at, updated_at, deleted_at
		from group_rule
		where group_id = ?
		  and id = ?
		  and deleted_at is null
		""";

	static final String EXISTS_TASK_COMPLETION_FOR_MEMBER = """
		select exists (
		  select 1
		  from task_completion
		  where id = ?
		    and member_id = ?
		)
		""";

	static final String EXISTS_TASK_COMPLETION = """
		select exists (
		  select 1
		  from task_completion
		  where id = ?
		)
		""";

	static final String SELECT_RULES_BY_GROUP_ID = """
		select id, group_id, created_by, rule_type, config, description, is_active,
		       created_at, updated_at, deleted_at
		from group_rule
		where group_id = ?
		  and deleted_at is null
		order by rule_type, created_at desc, id desc
		""";

	static final String INSERT_RULE = """
		insert into group_rule (
		  id, group_id, created_by, rule_type, config, description, is_active,
		  created_at, updated_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String UPDATE_RULE = """
		update group_rule
		set config = ?,
		    description = ?,
		    is_active = ?,
		    updated_at = ?
		where group_id = ?
		  and id = ?
		  and deleted_at is null
		""";

	static final String DEACTIVATE_RULE = """
		update group_rule
		set is_active = 0,
		    updated_at = ?
		where group_id = ?
		  and id = ?
		  and deleted_at is null
		""";

	static final String SOFT_DELETE_RULE = """
		update group_rule
		set is_active = 0,
		    deleted_at = ?,
		    updated_at = ?
		where group_id = ?
		  and id = ?
		  and deleted_at is null
		""";

	static final String SELECT_VIOLATION_BY_ID = """
		select rv.id, rv.rule_id, rv.member_id, rv.task_completion_id, rv.details,
		       rv.status, rv.resolved_at, rv.resolved_note, rv.occurred_at, rv.created_at
		from rule_violation rv
		join group_rule gr on gr.id = rv.rule_id
		where gr.group_id = ?
		  and rv.id = ?
		  and gr.deleted_at is null
		""";

	static final String SELECT_VIOLATIONS_BY_GROUP_ID = """
		select rv.id, rv.rule_id, rv.member_id, rv.task_completion_id, rv.details,
		       rv.status, rv.resolved_at, rv.resolved_note, rv.occurred_at, rv.created_at
		from rule_violation rv
		join group_rule gr on gr.id = rv.rule_id
		where gr.group_id = ?
		  and gr.deleted_at is null
		order by rv.occurred_at desc, rv.created_at desc, rv.id desc
		""";

	static final String INSERT_VIOLATION = """
		insert into rule_violation (
		  id, rule_id, member_id, task_completion_id, details, status,
		  resolved_at, resolved_note, occurred_at, created_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String UPDATE_VIOLATION_STATUS = """
		update rule_violation
		set status = ?,
		    resolved_at = ?,
		    resolved_note = ?
		where id = ?
		  and status = 'OPEN'
		""";

	private GroupRuleJdbcSql() {
	}
}
