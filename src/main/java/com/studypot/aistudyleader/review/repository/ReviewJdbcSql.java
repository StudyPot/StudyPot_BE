package com.studypot.aistudyleader.review.repository;

final class ReviewJdbcSql {

	static final String EXISTS_STUDY_GROUP = """
		select exists (
		  select 1
		  from study_group sg
		  where sg.id = ?
		    and sg.deleted_at is null
		)
		""";

	static final String SELECT_MEMBERSHIP = """
		select gm.group_id,
		       gm.id as member_id,
		       gm.user_id,
		       coalesce(nullif(gm.display_name, ''), u.nickname) as display_name,
		       gm.status as member_status
		from group_member gm
		join study_group sg on sg.id = gm.group_id
		left join users u on u.id = gm.user_id and u.deleted_at is null
		where gm.group_id = ?
		  and gm.user_id = ?
		  and sg.deleted_at is null
		  and gm.deleted_at is null
		""";

	static final String INSERT_REVIEW = """
		insert into group_review (
		  id, group_id, member_id, user_id, rating, content, created_at, updated_at
		) values (?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String SELECT_REVIEW_BY_GROUP_AND_USER = """
		select gr.id, gr.group_id, gr.member_id, gr.user_id,
		       coalesce(nullif(gm.display_name, ''), u.nickname) as display_name,
		       gr.rating, gr.content, gr.created_at, gr.updated_at
		from group_review gr
		join group_member gm on gm.id = gr.member_id
		left join users u on u.id = gr.user_id and u.deleted_at is null
		where gr.group_id = ?
		  and gr.user_id = ?
		  and gr.deleted_at is null
		""";

	static final String SELECT_REVIEWS_BY_GROUP = """
		select gr.id, gr.group_id, gr.member_id, gr.user_id,
		       coalesce(nullif(gm.display_name, ''), u.nickname) as display_name,
		       gr.rating, gr.content, gr.created_at, gr.updated_at
		from group_review gr
		join group_member gm on gm.id = gr.member_id
		left join users u on u.id = gr.user_id and u.deleted_at is null
		where gr.group_id = ?
		  and gr.deleted_at is null
		order by gr.created_at desc, gr.id desc
		""";

	static final String SELECT_RATING_COUNTS_BY_GROUP = """
		select gr.rating, count(*) as rating_count
		from group_review gr
		where gr.group_id = ?
		  and gr.deleted_at is null
		group by gr.rating
		""";

	private ReviewJdbcSql() {
	}
}
