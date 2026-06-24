package com.studypot.aistudyleader.studygroup.board.repository;

final class GroupBoardJdbcSql {

	static final String EXISTS_STUDY_GROUP = """
		select exists (
		  select 1
		  from study_group
		  where id = ?
		    and deleted_at is null
		)
		""";

	static final String SELECT_MEMBERSHIP = """
		select gm.group_id,
		       gm.id as member_id,
		       gm.user_id,
		       coalesce(nullif(gm.display_name, ''), u.nickname) as display_name,
		       gm.permission,
		       gm.status as member_status
		from group_member gm
		join study_group sg on sg.id = gm.group_id
		left join users u on u.id = gm.user_id and u.deleted_at is null
		where gm.group_id = ?
		  and gm.user_id = ?
		  and sg.deleted_at is null
		  and gm.deleted_at is null
		""";

	static final String SELECT_BOARDS_BY_GROUP_ID = """
		select id, group_id, board_type, name, description, display_order, is_default,
		       created_at, updated_at, deleted_at
		from group_board
		where group_id = ?
		  and deleted_at is null
		order by display_order asc, board_type asc
		""";

	static final String INSERT_DEFAULT_BOARD = """
		insert ignore into group_board (
		  id, group_id, board_type, name, description, display_order, is_default,
		  created_at, updated_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String SELECT_BOARD = """
		select id, group_id, board_type, name, description, display_order, is_default,
		       created_at, updated_at, deleted_at
		from group_board
		where group_id = ?
		  and id = ?
		  and deleted_at is null
		""";

	static final String EXISTS_BOARD = """
		select exists (
		  select 1
		  from group_board
		  where id = ?
		    and deleted_at is null
		)
		""";

	static final String INSERT_POST = """
		insert into group_board_post (
		  id, group_id, board_id, author_member_id, title, content, is_pinned, status,
		  created_at, updated_at
		) values (?, ?, ?, ?, ?, ?, ?, 'PUBLISHED', ?, ?)
		""";

	static final String SELECT_POSTS = """
		select p.id,
		       p.group_id,
		       p.board_id,
		       p.author_member_id,
		       gm.user_id as author_user_id,
		       coalesce(nullif(gm.display_name, ''), u.nickname) as author_display_name,
		       p.title,
		       case
		         when char_length(p.content) > 160 then concat(left(p.content, 160), '...')
		         else p.content
		       end as content_preview,
		       p.is_pinned,
		       (
		         select count(*)
		         from group_board_comment c
		         where c.post_id = p.id
		           and c.deleted_at is null
		           and c.status = 'PUBLISHED'
		       ) as comment_count,
		       p.created_at,
		       p.updated_at
		from group_board_post p
		left join group_member gm on gm.id = p.author_member_id
		left join users u on u.id = gm.user_id and u.deleted_at is null
		where p.group_id = ?
		  and p.board_id = ?
		  and p.deleted_at is null
		  and p.status = 'PUBLISHED'
		  and (
		    ? is null
		    or p.is_pinned < ?
		    or (p.is_pinned = ? and p.created_at < ?)
		    or (p.is_pinned = ? and p.created_at = ? and p.id < ?)
		  )
		""";
	// ORDER BY 와 LIMIT 은 리포지토리에서 GroupBoardPostSort 기준으로 동적으로 붙인다.

	// 그룹 전체 게시글(모든 게시판) 조회. board_id 조건만 빠지고 정렬/커서는 SELECT_POSTS 와 동일하다.
	static final String SELECT_ALL_POSTS = """
		select p.id,
		       p.group_id,
		       p.board_id,
		       p.author_member_id,
		       gm.user_id as author_user_id,
		       coalesce(nullif(gm.display_name, ''), u.nickname) as author_display_name,
		       p.title,
		       case
		         when char_length(p.content) > 160 then concat(left(p.content, 160), '...')
		         else p.content
		       end as content_preview,
		       p.is_pinned,
		       (
		         select count(*)
		         from group_board_comment c
		         where c.post_id = p.id
		           and c.deleted_at is null
		           and c.status = 'PUBLISHED'
		       ) as comment_count,
		       p.created_at,
		       p.updated_at
		from group_board_post p
		left join group_member gm on gm.id = p.author_member_id
		left join users u on u.id = gm.user_id and u.deleted_at is null
		where p.group_id = ?
		  and p.deleted_at is null
		  and p.status = 'PUBLISHED'
		  and (
		    ? is null
		    or p.is_pinned < ?
		    or (p.is_pinned = ? and p.created_at < ?)
		    or (p.is_pinned = ? and p.created_at = ? and p.id < ?)
		  )
		""";
	// ORDER BY 와 LIMIT 은 리포지토리에서 GroupBoardPostSort 기준으로 동적으로 붙인다.

	static final String SELECT_POST = """
		select p.id,
		       p.group_id,
		       p.board_id,
		       p.author_member_id,
		       gm.user_id as author_user_id,
		       coalesce(nullif(gm.display_name, ''), u.nickname) as author_display_name,
		       p.title,
		       p.content,
		       p.is_pinned,
		       p.created_at,
		       p.updated_at,
		       p.deleted_at
		from group_board_post p
		left join group_member gm on gm.id = p.author_member_id
		left join users u on u.id = gm.user_id and u.deleted_at is null
		where p.group_id = ?
		  and p.id = ?
		  and p.deleted_at is null
		  and p.status = 'PUBLISHED'
		""";

	static final String EXISTS_POST = """
		select exists (
		  select 1
		  from group_board_post
		  where id = ?
		    and deleted_at is null
		    and status = 'PUBLISHED'
		)
		""";

	static final String UPDATE_POST = """
		update group_board_post
		set title = ?,
		    content = ?,
		    is_pinned = ?,
		    updated_at = ?
		where group_id = ?
		  and id = ?
		  and deleted_at is null
		  and status = 'PUBLISHED'
		""";

	static final String SOFT_DELETE_POST = """
		update group_board_post
		set status = 'DELETED',
		    deleted_at = ?,
		    updated_at = ?
		where group_id = ?
		  and id = ?
		  and deleted_at is null
		  and status = 'PUBLISHED'
		""";

	static final String INSERT_COMMENT = """
		insert into group_board_comment (
		  id, group_id, post_id, author_member_id, content, status, created_at, updated_at
		) values (?, ?, ?, ?, ?, 'PUBLISHED', ?, ?)
		""";

	static final String SELECT_COMMENTS = """
		select c.id,
		       c.group_id,
		       c.post_id,
		       c.author_member_id,
		       gm.user_id as author_user_id,
		       coalesce(nullif(gm.display_name, ''), u.nickname) as author_display_name,
		       c.content,
		       c.created_at,
		       c.updated_at,
		       c.deleted_at
		from group_board_comment c
		left join group_member gm on gm.id = c.author_member_id
		left join users u on u.id = gm.user_id and u.deleted_at is null
		where c.group_id = ?
		  and c.post_id = ?
		  and c.deleted_at is null
		  and c.status = 'PUBLISHED'
		  and (
		    ? is null
		    or c.created_at > ?
		    or (c.created_at = ? and c.id > ?)
		  )
		order by c.created_at asc, c.id asc
		limit ?
		""";

	static final String SELECT_COMMENT = """
		select c.id,
		       c.group_id,
		       c.post_id,
		       c.author_member_id,
		       gm.user_id as author_user_id,
		       coalesce(nullif(gm.display_name, ''), u.nickname) as author_display_name,
		       c.content,
		       c.created_at,
		       c.updated_at,
		       c.deleted_at
		from group_board_comment c
		left join group_member gm on gm.id = c.author_member_id
		left join users u on u.id = gm.user_id and u.deleted_at is null
		where c.group_id = ?
		  and c.id = ?
		  and c.deleted_at is null
		  and c.status = 'PUBLISHED'
		""";

	static final String EXISTS_COMMENT = """
		select exists (
		  select 1
		  from group_board_comment
		  where id = ?
		    and deleted_at is null
		    and status = 'PUBLISHED'
		)
		""";

	static final String UPDATE_COMMENT = """
		update group_board_comment
		set content = ?,
		    updated_at = ?
		where group_id = ?
		  and id = ?
		  and deleted_at is null
		  and status = 'PUBLISHED'
		""";

	static final String SOFT_DELETE_COMMENT = """
		update group_board_comment
		set status = 'DELETED',
		    deleted_at = ?,
		    updated_at = ?
		where group_id = ?
		  and id = ?
		  and deleted_at is null
		  and status = 'PUBLISHED'
		""";

	private GroupBoardJdbcSql() {
	}
}
