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
		join group_member gm on gm.id = r.member_id
		where r.id = ?
		  and gm.group_id = c.group_id
		  and cw.deleted_at is null
		  and c.deleted_at is null
		  and gm.deleted_at is null
		""";

	static final String INSERT_CONVERSATION = """
		insert into ai_conversation (
		  id, group_id, member_id, curriculum_week_id, retrospective_id,
		  conversation_type, status, summary, opened_at, closed_at, created_at, updated_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	// v1 ai_conversation has no deleted_at column; deletion boundaries live on study_group/group_member.
	static final String EXISTS_CONVERSATION = """
		select exists (
		  select 1
		  from ai_conversation
		  where id = ?
		)
		""";

	static final String SELECT_MESSAGE_CONTEXT = """
		select ac.id as conversation_id, ac.group_id, ac.member_id, ac.status as conversation_status,
		       sg.status as group_status, gm.status as member_status
		from ai_conversation ac
		join study_group sg on sg.id = ac.group_id
		join group_member gm on gm.id = ac.member_id
		where ac.id = ?
		  and gm.group_id = ac.group_id
		  and gm.user_id = ?
		  and sg.deleted_at is null
		  and gm.deleted_at is null
		""";

	static final String INSERT_MESSAGE = """
		insert into ai_conversation_message (
		  id, conversation_id, llm_usage_id, sender_type, content, metadata, created_at
		) values (?, ?, ?, ?, ?, ?, ?)
		""";

	static final String SELECT_MESSAGES = """
		select m.id, m.conversation_id, m.llm_usage_id, m.sender_type, m.content, m.metadata, m.created_at
		from ai_conversation_message m
		where m.conversation_id = ?
		  and (? is null or m.created_at > ? or (m.created_at = ? and m.id > ?))
		order by m.created_at asc, m.id asc
		limit ?
		""";

	private AiConversationJdbcSql() {
	}
}
