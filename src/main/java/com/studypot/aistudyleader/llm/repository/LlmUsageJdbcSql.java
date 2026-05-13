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

	private LlmUsageJdbcSql() {
	}
}
