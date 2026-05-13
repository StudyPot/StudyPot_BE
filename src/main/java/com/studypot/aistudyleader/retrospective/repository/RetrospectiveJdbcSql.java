package com.studypot.aistudyleader.retrospective.repository;

final class RetrospectiveJdbcSql {

	static final String EXISTS_CURRICULUM_WEEK = """
		select exists (
		  select 1
		  from curriculum_week
		  where id = ?
		    and deleted_at is null
		)
		""";

	static final String SELECT_WEEK_MEMBERSHIP = """
		select sg.id as group_id, gm.id as member_id, sg.status as group_status,
		       gm.permission, gm.status as member_status
		from curriculum_week cw
		join curriculum c on c.id = cw.curriculum_id
		join study_group sg on sg.id = c.group_id
		join group_member gm on gm.group_id = sg.id
		where cw.id = ?
		  and gm.user_id = ?
		  and cw.deleted_at is null
		  and c.deleted_at is null
		  and sg.deleted_at is null
		  and gm.deleted_at is null
		""";

	static final String SELECT_PROGRESS = """
		select id, curriculum_week_id, member_id, status, started_at, due_at, completed_at,
		       completion_note, incomplete_reason, reason_submitted_at
		from member_week_progress
		where curriculum_week_id = ?
		  and member_id = ?
		""";

	static final String SELECT_RETROSPECTIVE = """
		select id, progress_id, curriculum_week_id, member_id, llm_usage_id, trigger_type,
		       input_summary, ai_feedback, next_week_adjustment, status, requested_at,
		       completed_at, created_at, updated_at
		from retrospective
		where progress_id = ?
		  and curriculum_week_id = ?
		  and member_id = ?
		order by requested_at desc, id desc
		limit 1
		""";

	static final String SELECT_RETROSPECTIVE_BY_ID = """
		select id, progress_id, curriculum_week_id, member_id, llm_usage_id, trigger_type,
		       input_summary, ai_feedback, next_week_adjustment, status, requested_at,
		       completed_at, created_at, updated_at
		from retrospective
		where id = ?
		""";

	static final String SELECT_TASK_SUMMARIES = """
		select wt.id as task_id, wt.display_order, wt.task_type, wt.title, wt.required,
		       wt.due_at, coalesce(tc.status, 'TODO') as completion_status, tc.completed_at,
		       tc.completion_note, tc.incomplete_reason, tc.reason_submitted_at
		from weekly_task wt
		left join task_completion tc on tc.weekly_task_id = wt.id
		  and tc.progress_id = ?
		  and tc.member_id = ?
		where wt.curriculum_week_id = ?
		  and wt.deleted_at is null
		order by wt.display_order
		""";

	static final String SELECT_ONBOARDING_SUMMARY = """
		select id, keyword_skill_levels, task_preferences, additional_note, status, submitted_at
		from group_onboarding_response
		where group_id = ?
		  and member_id = ?
		  and deleted_at is null
		order by updated_at desc, id desc
		limit 1
		""";

	static final String SELECT_ACTIVE_RULE_SUMMARIES = """
		select id, rule_type, config, description, is_active
		from group_rule
		where group_id = ?
		  and is_active = 1
		  and deleted_at is null
		order by rule_type, created_at desc, id desc
		""";

	static final String SELECT_RULE_VIOLATION_SUMMARIES = """
		select rv.id, rv.rule_id, gr.rule_type, rv.task_completion_id, rv.details,
		       rv.status, rv.resolved_at, rv.resolved_note, rv.occurred_at
		from rule_violation rv
		join group_rule gr on gr.id = rv.rule_id
		where gr.group_id = ?
		  and rv.member_id = ?
		  and gr.deleted_at is null
		order by rv.occurred_at desc, rv.created_at desc, rv.id desc
		limit 20
		""";

	static final String SELECT_PRIOR_RETROSPECTIVES = """
		select id, curriculum_week_id, status, ai_feedback, next_week_adjustment,
		       requested_at, completed_at
		from retrospective
		where member_id = ?
		  and id <> ?
		  and curriculum_week_id <> ?
		  and status in ('COMPLETED','FAILED')
		order by requested_at desc, id desc
		limit ?
		""";

	static final String SELECT_RETROSPECTIVE_CONVERSATION_SUMMARY = """
		select id, status, summary, opened_at, closed_at
		from ai_conversation
		where retrospective_id = ?
		  and member_id = ?
		  and summary is not null
		  and summary <> ''
		order by updated_at desc, id desc
		limit 1
		""";

	static final String INSERT_RETROSPECTIVE = """
		insert into retrospective (
		  id, progress_id, curriculum_week_id, member_id, llm_usage_id, trigger_type,
		  input_summary, ai_feedback, next_week_adjustment, status, requested_at,
		  completed_at, created_at, updated_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String UPDATE_RETROSPECTIVE_RESULT = """
		update retrospective
		set llm_usage_id = ?,
		    ai_feedback = ?,
		    next_week_adjustment = ?,
		    status = ?,
		    completed_at = ?,
		    updated_at = ?
		where id = ?
		""";

	private RetrospectiveJdbcSql() {
	}
}
