package com.studypot.aistudyleader.review;

final class ReviewJdbcSql {

	static final String INSERT_REVIEW = """
		insert into study_group_review (
		  id, group_id, author_id, rating, content, created_at, updated_at
		) values (?, ?, ?, ?, ?, ?, ?)
		""";

	static final String EXISTS_BY_TARGET_ID_AND_AUTHOR_ID = """
		select exists (
		  select 1
		  from study_group_review
		  where group_id = ?
		    and author_id = ?
		    and deleted_at is null
		)
		""";

	static final String SELECT_BY_ID = """
		select id, group_id, author_id, rating, content, created_at, updated_at
		from study_group_review
		where id = ?
		  and deleted_at is null
		""";

	static final String SELECT_BY_TARGET_ID_AND_AUTHOR_ID = """
		select id, group_id, author_id, rating, content, created_at, updated_at
		from study_group_review
		where group_id = ?
		  and author_id = ?
		  and deleted_at is null
		""";

	static final String SELECT_BY_TARGET_ID_ORDER_BY_CREATED_AT_DESC = """
		select id, group_id, author_id, rating, content, created_at, updated_at
		from study_group_review
		where group_id = ?
		  and deleted_at is null
		order by created_at desc, id desc
		""";

	static final String SOFT_DELETE_REVIEW = """
		update study_group_review
		set deleted_at = ?,
		    updated_at = ?
		where id = ?
		  and deleted_at is null
		""";

	private ReviewJdbcSql() {
	}
}
