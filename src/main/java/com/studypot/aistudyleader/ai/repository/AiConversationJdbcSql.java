package com.studypot.aistudyleader.ai.repository;

final class AiConversationJdbcSql {

	static final String EXISTS_STUDY_GROUP = """
		select exists (
		  select 1
		  from study_group
		  where id = ?
		    and deleted_at is null
		)
		""";

	static final String SELECT_MEMBERSHIP = """
		select sg.id as group_id, gm.id as member_id, sg.status as group_status,
		       gm.permission, gm.status as member_status
		from study_group sg
		join group_member gm on gm.group_id = sg.id
		where sg.id = ?
		  and gm.user_id = ?
		  and sg.deleted_at is null
		  and gm.deleted_at is null
		""";

	static final String SELECT_WEEK_GROUP_ID = """
		select c.group_id
		from curriculum_week cw
		join curriculum c on c.id = cw.curriculum_id
		where cw.id = ?
		  and cw.deleted_at is null
		  and c.deleted_at is null
		""";

	static final String SELECT_RETROSPECTIVE_REFERENCE = """
		select c.group_id, r.member_id, r.curriculum_week_id
		from retrospective r
		join curriculum_week cw on cw.id = r.curriculum_week_id
		join curriculum c on c.id = cw.curriculum_id
		where r.id = ?
		  and cw.deleted_at is null
		  and c.deleted_at is null
		""";

	static final String INSERT_CONVERSATION = """
		insert into ai_conversation (
		  id, group_id, member_id, curriculum_week_id, retrospective_id,
		  conversation_type, status, summary, opened_at, closed_at, created_at, updated_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	private AiConversationJdbcSql() {
	}
}
