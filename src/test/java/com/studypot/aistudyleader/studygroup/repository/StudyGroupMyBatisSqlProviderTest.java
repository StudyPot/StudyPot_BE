package com.studypot.aistudyleader.studygroup.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StudyGroupMyBatisSqlProviderTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000009001");
	private static final UUID CURSOR_ID = UUID.fromString("018f0000-0000-7000-8000-000000009002");
	private static final Instant CURSOR_CREATED_AT = Instant.parse("2026-06-01T00:00:00Z");

	private final StudyGroupMyBatisSqlProvider provider = new StudyGroupMyBatisSqlProvider();

	@Test
	void createStudyGroupContainsInsertValuesForCoreCrud() {
		String sql = provider.createStudyGroup();

		assertThat(sql)
			.contains("INSERT INTO study_group")
			.contains("id, created_by, name")
			.contains("detail_keywords")
			.contains("invite_code")
			.contains("#{now}");
	}

	@Test
	void searchStudyGroupsAddsDynamicFiltersSortAndCursorPagination() {
		String sql = provider.searchStudyGroups(Map.of(
			"viewerUserId", USER_ID,
			"keyword", "Spring",
			"status", "ACTIVE",
			"sort", "START_DATE_DESC",
			"cursorCreatedAt", CURSOR_CREATED_AT,
			"cursorId", CURSOR_ID,
			"limit", 11
		));

		assertThat(sql)
			.contains("FROM study_group sg")
			.contains("LEFT OUTER JOIN group_member gm")
			.contains("sg.name like concat('%', #{keyword}, '%')")
			.contains("sg.status = #{status}")
			.contains("sg.created_at < #{cursorCreatedAt}")
			.contains("ORDER BY sg.starts_at desc, sg.id desc")
			.contains("LIMIT #{limit}");
	}

	@Test
	void searchStudyGroupsFallsBackToCreatedSortWithoutOptionalFilters() {
		String sql = provider.searchStudyGroups(Map.of("viewerUserId", USER_ID, "sort", "CREATED_DESC", "limit", 10));

		assertThat(sql)
			.doesNotContain("#{keyword}")
			.doesNotContain("sg.status = #{status}")
			.contains("ORDER BY sg.created_at desc, sg.id desc");
	}

	@Test
	void findStudyGroupDetailJoinsRelatedDataForDetailPage() {
		String sql = provider.findStudyGroupDetail();

		assertThat(sql)
			.contains("LEFT OUTER JOIN group_member owner")
			.contains("LEFT OUTER JOIN group_member gm")
			.contains("LEFT OUTER JOIN study_group_review gr")
			.contains("favorite_group fg")
			.contains("sg.id = #{groupId}")
			.contains("review_count")
			.contains("average_rating");
	}

	@Test
	void updateAndDeleteStudyGroupContainOwnerGuardedCrudStatements() {
		assertThat(provider.updateStudyGroup())
			.contains("UPDATE study_group sg")
			.contains("INNER JOIN group_member gm")
			.contains("sg.name = #{name}")
			.contains("gm.permission = 'OWNER'")
			.contains("sg.deleted_at is null");

		assertThat(provider.deleteStudyGroup())
			.contains("UPDATE study_group sg")
			.contains("sg.deleted_at = #{deletedAt}")
			.contains("gm.permission = 'OWNER'")
			.contains("sg.deleted_at is null");
	}
}
