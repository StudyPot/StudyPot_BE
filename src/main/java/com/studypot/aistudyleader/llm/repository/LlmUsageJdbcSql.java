package com.studypot.aistudyleader.llm.repository;

final class LlmUsageJdbcSql {

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

	static final String INSERT_LLM_USAGE = """
		insert into llm_usage (
		  id, user_id, group_id, purpose, provider, model, input_tokens, output_tokens,
		  total_cost_usd, latency_ms, status, error_code, request_payload, response_summary,
		  created_date_utc, created_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String SELECT_GROUP_USAGE = """
		select id, user_id, group_id, purpose, provider, model, input_tokens, output_tokens,
		       total_cost_usd, latency_ms, status, error_code, request_payload,
		       response_summary, created_date_utc, created_at
		from llm_usage
		where group_id = ?
		order by created_at desc, id desc
		limit ?
		""";

	static final String SELECT_USER_USAGE = """
		select id, user_id, group_id, purpose, provider, model, input_tokens, output_tokens,
		       total_cost_usd, latency_ms, status, error_code, request_payload,
		       response_summary, created_date_utc, created_at
		from llm_usage
		where user_id = ?
		order by created_at desc, id desc
		limit ?
		""";

	static final String SELECT_USER_EMAIL = """
		select email
		from users
		where id = ?
		  and deleted_at is null
		""";

	/**
	 * 운영자 전용 목록 조회의 고정 SELECT/FROM 절입니다. WHERE 조건과 LIMIT은 동적으로 덧붙입니다.
	 */
	static final String SELECT_ADMIN_USAGE_PREFIX = """
		select u.id, u.user_id, usr.nickname as user_nickname, usr.email as user_email,
		       u.group_id, sg.name as group_name,
		       u.purpose, u.provider, u.model, u.input_tokens, u.output_tokens,
		       u.total_cost_usd, u.latency_ms, u.status, u.error_code,
		       u.request_payload, u.response_summary, u.created_at
		from llm_usage u
		left join users usr on usr.id = u.user_id
		left join study_group sg on sg.id = u.group_id
		""";

	static final String SELECT_ADMIN_USAGE_SUFFIX = """
		order by u.created_at desc, u.id desc
		limit ?
		""";

	/**
	 * 운영자 전용 집계 조회의 고정 SELECT/FROM 절입니다. WHERE 조건을 동적으로 덧붙입니다.
	 */
	static final String SELECT_ADMIN_SUMMARY_PREFIX = """
		select
		  count(*) as total_calls,
		  coalesce(sum(case when u.status = 'SUCCESS' then 1 else 0 end), 0) as success_calls,
		  coalesce(sum(case when u.status <> 'SUCCESS' then 1 else 0 end), 0) as failed_calls,
		  coalesce(sum(u.input_tokens), 0) as input_tokens,
		  coalesce(sum(u.output_tokens), 0) as output_tokens,
		  coalesce(sum(u.total_cost_usd), 0) as total_cost_usd
		from llm_usage u
		""";

	private LlmUsageJdbcSql() {
	}
}
