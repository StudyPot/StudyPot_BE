package com.studypot.aistudyleader.bookmark.repository;

final class BookmarkJdbcSql {

	static final String EXISTS_STUDY_GROUP = """
		select exists(
		  select 1 from study_group where id = ? and deleted_at is null
		)
		""";

	static final String SELECT_BOOKMARK_ACTIVE = """
		select (deleted_at is null) as active
		from group_bookmark
		where user_id = ? and group_id = ?
		""";

	static final String INSERT_BOOKMARK = """
		insert into group_bookmark (id, user_id, group_id, created_at, updated_at)
		values (?, ?, ?, ?, ?)
		""";

	static final String REACTIVATE_BOOKMARK = """
		update group_bookmark
		set deleted_at = null, created_at = ?, updated_at = ?
		where user_id = ? and group_id = ?
		""";

	static final String SOFT_DELETE_BOOKMARK = """
		update group_bookmark
		set deleted_at = ?, updated_at = ?
		where user_id = ? and group_id = ? and deleted_at is null
		""";

	static final String SELECT_MY_BOOKMARKS = """
		select
		  sg.id, sg.created_by, sg.name, sg.topic, sg.detail_keywords, sg.level,
		  sg.status, sg.max_members, sg.is_public, sg.invite_code, sg.starts_at, sg.ends_at,
		  sg.description, sg.onboarding_started_at, sg.started_at, sg.created_at, sg.updated_at,
		  gb.created_at as bookmarked_at
		from group_bookmark gb
		join study_group sg on sg.id = gb.group_id
		where gb.user_id = ?
		  and gb.deleted_at is null
		  and sg.deleted_at is null
		order by gb.created_at desc
		""";

	private BookmarkJdbcSql() {
	}
}
