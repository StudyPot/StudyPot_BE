package com.studypot.aistudyleader.studygroup.catalog;

final class StudyGroupCatalogJdbcSql {

	static final String INSERT_STUDY_GROUP = """
		insert into study_group_catalog (
		  id, name, topic, status, starts_at, ends_at, member_count, average_rating, favorite
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String SEARCH_STUDY_GROUPS = """
		select id, name, topic, status, starts_at, ends_at, member_count, average_rating, favorite
		from study_group_catalog
		where deleted_at is null
		  and (? is null or ? = '' or name like concat('%', ?, '%') or topic like concat('%', ?, '%'))
		  and (? is null or ? = '' or status = ?)
		  and (? is null or id > ?)
		order by
		  case when ? = 'name' then name end asc,
		  case when ? = 'startDate' then starts_at end desc,
		  favorite desc,
		  starts_at desc,
		  id asc
		limit ?
		""";

	static final String SELECT_DETAIL = """
		select id, name, topic, status, starts_at, ends_at, member_count, average_rating, favorite
		from study_group_catalog
		where id = ?
		  and deleted_at is null
		""";

	static final String UPDATE_STUDY_GROUP = """
		update study_group_catalog
		set name = ?,
		    topic = ?,
		    status = ?,
		    starts_at = ?,
		    ends_at = ?,
		    favorite = ?,
		    updated_at = current_timestamp(6)
		where id = ?
		  and deleted_at is null
		""";

	static final String SOFT_DELETE_STUDY_GROUP = """
		update study_group_catalog
		set deleted_at = current_timestamp(6),
		    updated_at = current_timestamp(6)
		where id = ?
		  and deleted_at is null
		""";

	private StudyGroupCatalogJdbcSql() {
	}
}
