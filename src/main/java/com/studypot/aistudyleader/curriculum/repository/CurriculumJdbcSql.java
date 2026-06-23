package com.studypot.aistudyleader.curriculum.repository;

final class CurriculumJdbcSql {

	static final String EXISTS_STUDY_GROUP = """
		select exists (
		  select 1
		  from study_group
		  where id = ?
		    and deleted_at is null
		)
		""";

	static final String EXISTS_CURRICULUM_WEEK = """
		select exists (
		  select 1
		  from curriculum_week
		  where id = ?
		    and deleted_at is null
		)
		""";

	static final String EXISTS_WEEKLY_TASK = """
		select exists (
		  select 1
		  from weekly_task
		  where id = ?
		    and deleted_at is null
		)
		""";

	static final String SELECT_START_CONTEXT = """
		select sg.id as group_id, sg.name as group_name, sg.topic, sg.detail_keywords,
		       sg.status as group_status, sg.starts_at, sg.ends_at,
		       gm.id as member_id, gm.permission, gm.status as member_status
		from study_group sg
		join group_member gm on gm.group_id = sg.id
		where sg.id = ?
		  and gm.user_id = ?
		  and sg.deleted_at is null
		  and gm.deleted_at is null
		""";

	static final String SELECT_WEEK_READ_CONTEXT = """
		select sg.id as group_id, sg.name as group_name, sg.topic, sg.detail_keywords,
		       sg.status as group_status, sg.starts_at, sg.ends_at,
		       gm.id as member_id, gm.permission, gm.status as member_status
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

	static final String SELECT_TASK_READ_CONTEXT = """
		select sg.id as group_id, sg.name as group_name, sg.topic, sg.detail_keywords,
		       sg.status as group_status, sg.starts_at, sg.ends_at,
		       gm.id as member_id, gm.permission, gm.status as member_status
		from weekly_task wt
		join curriculum_week cw on cw.id = wt.curriculum_week_id
		join curriculum c on c.id = cw.curriculum_id
		join study_group sg on sg.id = c.group_id
		join group_member gm on gm.group_id = sg.id
		where wt.id = ?
		  and gm.user_id = ?
		  and wt.deleted_at is null
		  and cw.deleted_at is null
		  and c.deleted_at is null
		  and sg.deleted_at is null
		  and gm.deleted_at is null
		""";

	static final String SELECT_SUBMITTED_ONBOARDING_RESPONSES = """
		select gor.id, gor.member_id, gor.keyword_skill_levels, gor.task_preferences,
		       gor.additional_note, gor.submitted_at
		from group_onboarding_response gor
		where gor.group_id = ?
		  and gor.status = 'SUBMITTED'
		  and gor.deleted_at is null
		order by gor.submitted_at, gor.id
		""";

	static final String SELECT_SUBMITTED_AVAILABILITY_SLOTS = """
		select mas.onboarding_response_id, mas.day_of_week, mas.start_time, mas.end_time, mas.timezone
		from member_availability_slot mas
		join group_onboarding_response gor on gor.id = mas.onboarding_response_id
		where gor.group_id = ?
		  and gor.status = 'SUBMITTED'
		  and gor.deleted_at is null
		  and mas.deleted_at is null
		order by mas.onboarding_response_id, mas.day_of_week, mas.start_time, mas.end_time
		""";

	static final String UPDATE_STUDY_GROUP_STARTED = """
		update study_group
		set status = 'ACTIVE',
		    started_at = ?,
		    updated_at = ?
		where id = ?
		  and status = 'READY_TO_START'
		  and deleted_at is null
		""";

	static final String INSERT_LLM_USAGE = """
		insert into llm_usage (
		  id, user_id, group_id, purpose, provider, model, input_tokens, output_tokens,
		  total_cost_usd, latency_ms, status, error_code, request_payload, response_summary,
		  created_date_utc, created_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String INSERT_CURRICULUM = """
		insert into curriculum (
		  id, group_id, llm_usage_id, title, total_weeks, onboarding_summary,
		  generated_by_ai, generation_prompt, status, created_at, updated_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String INSERT_CURRICULUM_WEEK = """
		insert into curriculum_week (
		  id, curriculum_id, week_number, title, description, sprint_goal, retrospective_questions,
		  learning_goals, resources, status, starts_at, ends_at, created_at, updated_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String INSERT_WEEKLY_TASK = """
		insert into weekly_task (
		  id, curriculum_week_id, display_order, task_type, title, description, required,
		  due_at, generated_by_ai, source_payload, created_at, updated_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String SELECT_ACTIVE_CURRICULUM = """
		select id, group_id, llm_usage_id, title, total_weeks, onboarding_summary,
		       generated_by_ai, generation_prompt, status, created_at, updated_at
		from curriculum
		where group_id = ?
		  and status = 'ACTIVE'
		  and deleted_at is null
		""";

	static final String SELECT_CURRICULUM_WEEKS = """
		select id, curriculum_id, week_number, title, description, sprint_goal, retrospective_questions,
		       learning_goals, resources, status, starts_at, ends_at, created_at, updated_at
		from curriculum_week
		where curriculum_id = ?
		  and deleted_at is null
		order by week_number
		""";

	static final String SELECT_WEEKLY_TASKS_BY_CURRICULUM = """
		select wt.id, wt.curriculum_week_id, wt.display_order, wt.task_type, wt.title,
		       wt.description, wt.required, wt.due_at, wt.generated_by_ai, wt.source_payload,
		       wt.created_at, wt.updated_at
		from weekly_task wt
		join curriculum_week cw on cw.id = wt.curriculum_week_id
		where cw.curriculum_id = ?
		  and cw.deleted_at is null
		  and wt.deleted_at is null
		order by cw.week_number, wt.display_order
		""";

	static final String SELECT_CURRENT_WEEK_BY_GROUP = """
		select cw.id, cw.curriculum_id, cw.week_number, cw.title, cw.description,
		       cw.sprint_goal, cw.retrospective_questions, cw.learning_goals, cw.resources, cw.status,
		       cw.starts_at, cw.ends_at, cw.created_at, cw.updated_at
		from curriculum_week cw
		join curriculum c on c.id = cw.curriculum_id
		where c.group_id = ?
		  and c.status = 'ACTIVE'
		  and cw.status = 'IN_PROGRESS'
		  and c.deleted_at is null
		  and cw.deleted_at is null
		order by cw.week_number
		limit 1
		""";

	static final String SELECT_WEEKS_BY_GROUP = """
		select cw.id, cw.curriculum_id, cw.week_number, cw.title, cw.description,
		       cw.sprint_goal, cw.retrospective_questions, cw.learning_goals, cw.resources, cw.status,
		       cw.starts_at, cw.ends_at, cw.created_at, cw.updated_at
		from curriculum_week cw
		join curriculum c on c.id = cw.curriculum_id
		where c.group_id = ?
		  and c.status = 'ACTIVE'
		  and c.deleted_at is null
		  and cw.deleted_at is null
		order by cw.week_number
		""";

	static final String SELECT_WEEK_BY_ID = """
		select id, curriculum_id, week_number, title, description, sprint_goal, retrospective_questions,
		       learning_goals, resources, status, starts_at, ends_at, created_at, updated_at
		from curriculum_week
		where id = ?
		  and deleted_at is null
		""";

	static final String SELECT_NEXT_PENDING_WEEK = """
		select nw.id, nw.week_number, nw.title, nw.sprint_goal
		from curriculum_week cw
		join curriculum_week nw on nw.curriculum_id = cw.curriculum_id
		  and nw.week_number = cw.week_number + 1
		where cw.id = ?
		  and cw.deleted_at is null
		  and nw.deleted_at is null
		  and nw.status = 'PENDING'
		limit 1
		""";

	// 자동 재생성용: 다음 주차가 이미 IN_PROGRESS 로 전환됐어도(주차 전이 스케줄러와의 경합) 아직 마감 전이면 대상.
	static final String SELECT_NEXT_REGENERABLE_WEEK = """
		select nw.id, nw.week_number, nw.title, nw.sprint_goal
		from curriculum_week cw
		join curriculum_week nw on nw.curriculum_id = cw.curriculum_id
		  and nw.week_number = cw.week_number + 1
		where cw.id = ?
		  and cw.deleted_at is null
		  and nw.deleted_at is null
		  and nw.status <> 'COMPLETED'
		  and nw.ends_at > ?
		limit 1
		""";

	static final String SELECT_LATEST_WEEKLY_REPORT_BODY = """
		select content
		from group_board_post
		where group_id = ?
		  and title like '%주차 학습 리포트'
		  and status = 'PUBLISHED'
		  and deleted_at is null
		order by created_at desc, id desc
		limit 1
		""";

	static final String SOFT_DELETE_WEEK_TASKS = """
		update weekly_task
		set deleted_at = ?,
		    updated_at = ?
		where curriculum_week_id = ?
		  and deleted_at is null
		""";

	static final String UPDATE_WEEK_RETROSPECTIVE_QUESTIONS = """
		update curriculum_week
		set retrospective_questions = ?,
		    updated_at = ?
		where id = ?
		  and deleted_at is null
		""";

	static final String SELECT_WEEKLY_TASKS_BY_WEEK = """
		select wt.id, wt.curriculum_week_id, wt.display_order, wt.task_type, wt.title,
		       wt.description, wt.required, wt.due_at, wt.generated_by_ai, wt.source_payload,
		       wt.created_at, wt.updated_at
		from weekly_task wt
		where wt.curriculum_week_id = ?
		  and wt.deleted_at is null
		order by wt.display_order
		""";

	static final String SELECT_WEEKLY_TASK_BY_ID = """
		select id, curriculum_week_id, display_order, task_type, title, description,
		       required, due_at, generated_by_ai, source_payload, created_at, updated_at
		from weekly_task
		where id = ?
		  and deleted_at is null
		""";

	static final String SELECT_CURRICULUM_WEEK_STARTS_AT = """
		select starts_at
		from curriculum_week
		where id = ?
		  and deleted_at is null
		""";

	static final String SELECT_MEMBER_WEEK_PROGRESS_BY_WEEK_AND_MEMBER = """
		select id, curriculum_week_id, member_id, status, started_at, due_at, completed_at,
		       completion_note, incomplete_reason, reason_submitted_at, created_at, updated_at
		from member_week_progress
		where curriculum_week_id = ?
		  and member_id = ?
		""";

	static final String SELECT_TASK_COMPLETION_BY_TASK_AND_MEMBER = """
		select id, progress_id, weekly_task_id, member_id, status, due_at, completed_at,
		       completion_note, incomplete_reason, reason_submitted_at, evidence_url, created_at, updated_at
		from task_completion
		where weekly_task_id = ?
		  and member_id = ?
		""";

	static final String SELECT_TASK_COMPLETIONS_BY_WEEK_AND_MEMBER = """
		select tc.id, tc.progress_id, tc.weekly_task_id, tc.member_id, tc.status, tc.due_at,
		       tc.completed_at, tc.completion_note, tc.incomplete_reason, tc.reason_submitted_at,
		       tc.evidence_url, tc.created_at, tc.updated_at
		from weekly_task wt
		join task_completion tc on tc.weekly_task_id = wt.id
		where wt.curriculum_week_id = ?
		  and tc.member_id = ?
		  and wt.deleted_at is null
		order by wt.display_order
		""";

	static final String SELECT_WEEK_DUE_AT = """
		select ends_at
		from curriculum_week
		where id = ?
		  and deleted_at is null
		""";

	static final String INSERT_TASK_COMPLETION = """
		insert into task_completion (
		  id, progress_id, weekly_task_id, member_id, status, due_at, completed_at,
		  completion_note, incomplete_reason, reason_submitted_at, evidence_url, created_at, updated_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String UPDATE_TASK_COMPLETION = """
		update task_completion
		set status = ?,
		    completed_at = ?,
		    completion_note = ?,
		    incomplete_reason = ?,
		    reason_submitted_at = ?,
		    evidence_url = ?,
		    updated_at = ?
		where id = ?
		""";

	static final String INSERT_MEMBER_WEEK_PROGRESS = """
		insert into member_week_progress (
		  id, curriculum_week_id, member_id, status, started_at, due_at, completed_at,
		  completion_note, incomplete_reason, reason_submitted_at, created_at, updated_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String UPDATE_MEMBER_WEEK_PROGRESS = """
		update member_week_progress
		set status = ?,
		    started_at = ?,
		    due_at = ?,
		    completed_at = ?,
		    completion_note = ?,
		    incomplete_reason = ?,
		    reason_submitted_at = ?,
		    updated_at = ?
		where id = ?
		""";

	static final String COUNT_ACTIVE_OR_ONBOARDING_MEMBERS = """
		select count(*)
		from group_member gm
		where gm.group_id = ?
		  and gm.status in ('PENDING_ONBOARDING', 'ACTIVE')
		  and gm.deleted_at is null
		""";

	// 활동 잔디 집계: 완료(DONE)한 todo + 작성한 게시글을 일자별로 합산해 멤버별 활동 개수를 낸다.
	// 활동이 없는 멤버도 LEFT JOIN 으로 포함(date null, count 0). 파라미터: (from,to,from,to,groupId)
	static final String SELECT_GROUP_DONE_ACTIVITY_COUNTS = """
		select
		  gm.id as member_id,
		  gm.user_id,
		  coalesce(nullif(gm.display_name, ''), u.nickname) as display_name,
		  u.nickname,
		  act.activity_date as activity_date,
		  count(act.activity_id) as activity_count
		from group_member gm
		join users u on u.id = gm.user_id
		left join (
		  select tc.member_id as member_id,
		         date(tc.completed_at) as activity_date,
		         tc.id as activity_id
		  from task_completion tc
		  where tc.status = 'DONE'
		    and tc.completed_at >= ?
		    and tc.completed_at < ?
		  union all
		  select p.author_member_id as member_id,
		         date(p.created_at) as activity_date,
		         p.id as activity_id
		  from group_board_post p
		  where p.deleted_at is null
		    and p.status = 'PUBLISHED'
		    and p.created_at >= ?
		    and p.created_at < ?
		) act on act.member_id = gm.id
		where gm.group_id = ?
		  and gm.status in ('PENDING_ONBOARDING', 'ACTIVE')
		  and gm.deleted_at is null
		group by gm.id, gm.user_id, display_name, u.nickname, act.activity_date
		order by gm.joined_at asc, gm.id asc
		""";

	private CurriculumJdbcSql() {
	}
}
