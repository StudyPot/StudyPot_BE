package com.studypot.aistudyleader.studygroup.repository;

import java.util.Map;
import org.apache.ibatis.jdbc.SQL;

public final class StudyGroupMyBatisSqlProvider {

	public String createStudyGroup() {
		return new SQL()
			.INSERT_INTO("study_group")
			.VALUES("id", "#{id}")
			.VALUES("created_by", "#{createdBy}")
			.VALUES("name", "#{name}")
			.VALUES("description", "#{description}")
			.VALUES("topic", "#{topic}")
			.VALUES("detail_keywords", "#{detailKeywordsJson}")
			.VALUES("status", "'ONBOARDING'")
			.VALUES("max_members", "#{maxMembers}")
			.VALUES("is_public", "false")
			.VALUES("invite_code", "#{inviteCode}")
			.VALUES("starts_at", "#{startsAt}")
			.VALUES("ends_at", "#{endsAt}")
			.VALUES("created_at", "#{now}")
			.VALUES("updated_at", "#{now}")
			.toString();
	}

	public String searchStudyGroups(Map<String, Object> params) {
		String keyword = stringParam(params, "keyword");
		String status = stringParam(params, "status");
		return new SQL() {{
			SELECT("sg.id, sg.name, sg.topic, sg.detail_keywords, sg.status, sg.max_members");
			SELECT("sg.starts_at, sg.ends_at, sg.created_at, sg.updated_at");
			SELECT("count(distinct gm.id) as member_count");
			SELECT("exists(select 1 from favorite_group fg where fg.group_id = sg.id and fg.user_id = #{viewerUserId} and fg.deleted_at is null) as favorite");
			FROM("study_group sg");
			LEFT_OUTER_JOIN("group_member gm on gm.group_id = sg.id and gm.deleted_at is null");
			WHERE("sg.deleted_at is null");
			if (!keyword.isBlank()) {
				WHERE("(sg.name like concat('%', #{keyword}, '%') or sg.topic like concat('%', #{keyword}, '%'))");
			}
			if (!status.isBlank()) {
				WHERE("sg.status = #{status}");
			}
			if (params.get("cursorCreatedAt") != null && params.get("cursorId") != null) {
				WHERE("(sg.created_at < #{cursorCreatedAt} or (sg.created_at = #{cursorCreatedAt} and sg.id < #{cursorId}))");
			}
			GROUP_BY("sg.id");
			ORDER_BY(orderBy(stringParam(params, "sort")));
			LIMIT("#{limit}");
		}}.toString();
	}

	public String findStudyGroupDetail() {
		return new SQL() {{
			SELECT("sg.id, sg.created_by, sg.name, sg.description, sg.topic, sg.detail_keywords");
			SELECT("sg.status, sg.max_members, sg.starts_at, sg.ends_at, sg.created_at, sg.updated_at");
			SELECT("owner.display_name as owner_display_name");
			SELECT("count(distinct gm.id) as member_count");
			SELECT("count(distinct gr.id) as review_count");
			SELECT("coalesce(avg(gr.rating), 0) as average_rating");
			SELECT("exists(select 1 from favorite_group fg where fg.group_id = sg.id and fg.user_id = #{viewerUserId} and fg.deleted_at is null) as favorite");
			FROM("study_group sg");
			LEFT_OUTER_JOIN("group_member owner on owner.group_id = sg.id and owner.permission = 'OWNER' and owner.deleted_at is null");
			LEFT_OUTER_JOIN("group_member gm on gm.group_id = sg.id and gm.deleted_at is null");
			LEFT_OUTER_JOIN("study_group_review gr on gr.group_id = sg.id and gr.deleted_at is null");
			WHERE("sg.id = #{groupId}");
			WHERE("sg.deleted_at is null");
			GROUP_BY("sg.id, owner.display_name");
		}}.toString();
	}

	public String updateStudyGroup() {
		return new SQL()
			.UPDATE("study_group sg")
			.INNER_JOIN("group_member gm on gm.group_id = sg.id and gm.user_id = #{editorUserId}")
			.SET("sg.name = #{name}")
			.SET("sg.description = #{description}")
			.SET("sg.topic = #{topic}")
			.SET("sg.detail_keywords = #{detailKeywordsJson}")
			.SET("sg.max_members = #{maxMembers}")
			.SET("sg.starts_at = #{startsAt}")
			.SET("sg.ends_at = #{endsAt}")
			.SET("sg.updated_at = #{updatedAt}")
			.WHERE("sg.id = #{groupId}")
			.WHERE("gm.permission = 'OWNER'")
			.WHERE("gm.deleted_at is null")
			.WHERE("sg.deleted_at is null")
			.toString();
	}

	public String deleteStudyGroup() {
		return new SQL()
			.UPDATE("study_group sg")
			.INNER_JOIN("group_member gm on gm.group_id = sg.id and gm.user_id = #{ownerUserId}")
			.SET("sg.deleted_at = #{deletedAt}")
			.SET("sg.updated_at = #{deletedAt}")
			.WHERE("sg.id = #{groupId}")
			.WHERE("gm.permission = 'OWNER'")
			.WHERE("gm.deleted_at is null")
			.WHERE("sg.deleted_at is null")
			.toString();
	}

	private static String orderBy(String sort) {
		return switch (sort) {
			case "NAME_ASC" -> "sg.name asc, sg.id desc";
			case "START_DATE_DESC" -> "sg.starts_at desc, sg.id desc";
			default -> "sg.created_at desc, sg.id desc";
		};
	}

	private static String stringParam(Map<String, Object> params, String key) {
		Object value = params.get(key);
		return value instanceof String text ? text.trim() : "";
	}
}
