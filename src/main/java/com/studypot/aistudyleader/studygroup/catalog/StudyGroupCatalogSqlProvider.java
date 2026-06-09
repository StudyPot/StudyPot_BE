package com.studypot.aistudyleader.studygroup.catalog;

import java.util.Map;
import org.apache.ibatis.jdbc.SQL;

public final class StudyGroupCatalogSqlProvider {

	public String insertStudyGroup() {
		return new SQL()
			.INSERT_INTO("study_group")
			.VALUES("id", "#{id}")
			.VALUES("name", "#{name}")
			.VALUES("topic", "#{topic}")
			.VALUES("status", "#{status}")
			.VALUES("starts_at", "#{startsAt}")
			.VALUES("ends_at", "#{endsAt}")
			.toString();
	}

	public String searchStudyGroups(Map<String, Object> params) {
		String keyword = text(params, "keyword");
		String status = text(params, "status");
		return new SQL() {{
			SELECT("sg.id, sg.name, sg.topic, sg.status, sg.starts_at, sg.ends_at");
			SELECT("count(distinct gm.id) as member_count");
			SELECT("coalesce(avg(sgr.rating), 0) as average_rating");
			SELECT("exists(select 1 from favorite_group fg where fg.group_id = sg.id and fg.deleted_at is null) as favorite");
			FROM("study_group sg");
			LEFT_OUTER_JOIN("group_member gm on gm.group_id = sg.id and gm.deleted_at is null");
			LEFT_OUTER_JOIN("study_group_review sgr on sgr.group_id = sg.id and sgr.deleted_at is null");
			WHERE("sg.deleted_at is null");
			if (!keyword.isBlank()) {
				WHERE("(sg.name like concat('%', #{keyword}, '%') or sg.topic like concat('%', #{keyword}, '%'))");
			}
			if (!status.isBlank()) {
				WHERE("sg.status = #{status}");
			}
			if (!text(params, "cursor").isBlank()) {
				WHERE("sg.id > #{cursor}");
			}
			GROUP_BY("sg.id");
			ORDER_BY(orderBy(text(params, "sort")));
			LIMIT("#{pageSize}");
		}}.toString();
	}

	public String findStudyGroupDetail() {
		return new SQL() {{
			SELECT("sg.id, sg.name, sg.topic, sg.status, sg.starts_at, sg.ends_at");
			SELECT("count(distinct gm.id) as member_count");
			SELECT("coalesce(avg(sgr.rating), 0) as average_rating");
			SELECT("exists(select 1 from favorite_group fg where fg.group_id = sg.id and fg.deleted_at is null) as favorite");
			FROM("study_group sg");
			LEFT_OUTER_JOIN("group_member gm on gm.group_id = sg.id and gm.deleted_at is null");
			LEFT_OUTER_JOIN("study_group_review sgr on sgr.group_id = sg.id and sgr.deleted_at is null");
			WHERE("sg.id = #{groupId}");
			WHERE("sg.deleted_at is null");
			GROUP_BY("sg.id");
		}}.toString();
	}

	public String updateStudyGroup() {
		return new SQL()
			.UPDATE("study_group")
			.SET("name = #{command.name}")
			.SET("topic = #{command.topic}")
			.SET("status = #{command.status}")
			.SET("starts_at = #{command.startsAt}")
			.SET("ends_at = #{command.endsAt}")
			.WHERE("id = #{groupId}")
			.WHERE("deleted_at is null")
			.toString();
	}

	public String deleteStudyGroup() {
		return new SQL()
			.UPDATE("study_group")
			.SET("deleted_at = current_timestamp")
			.WHERE("id = #{groupId}")
			.WHERE("deleted_at is null")
			.toString();
	}

	private static String orderBy(String sort) {
		return switch (sort) {
			case "name" -> "sg.name asc, sg.id asc";
			case "startDate" -> "sg.starts_at desc, sg.id asc";
			default -> "favorite desc, sg.starts_at desc, sg.id asc";
		};
	}

	private static String text(Map<String, Object> params, String key) {
		Object value = params.get(key);
		return value instanceof String string ? string.strip() : "";
	}
}
