package com.studypot.aistudyleader.studygroup.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;

@Mapper
public interface StudyGroupMyBatisMapper {

	@InsertProvider(type = StudyGroupMyBatisSqlProvider.class, method = "createStudyGroup")
	int createStudyGroup(
		@Param("id") UUID id,
		@Param("createdBy") UUID createdBy,
		@Param("name") String name,
		@Param("description") String description,
		@Param("topic") String topic,
		@Param("detailKeywordsJson") String detailKeywordsJson,
		@Param("maxMembers") int maxMembers,
		@Param("startsAt") LocalDate startsAt,
		@Param("endsAt") LocalDate endsAt,
		@Param("inviteCode") String inviteCode,
		@Param("now") Instant now
	);

	@SelectProvider(type = StudyGroupMyBatisSqlProvider.class, method = "searchStudyGroups")
	List<Map<String, Object>> searchStudyGroups(
		@Param("viewerUserId") UUID viewerUserId,
		@Param("keyword") String keyword,
		@Param("status") String status,
		@Param("sort") String sort,
		@Param("cursorCreatedAt") Instant cursorCreatedAt,
		@Param("cursorId") UUID cursorId,
		@Param("limit") int limit
	);

	@SelectProvider(type = StudyGroupMyBatisSqlProvider.class, method = "findStudyGroupDetail")
	Map<String, Object> findStudyGroupDetail(@Param("groupId") UUID groupId, @Param("viewerUserId") UUID viewerUserId);

	@UpdateProvider(type = StudyGroupMyBatisSqlProvider.class, method = "updateStudyGroup")
	int updateStudyGroup(
		@Param("groupId") UUID groupId,
		@Param("editorUserId") UUID editorUserId,
		@Param("name") String name,
		@Param("description") String description,
		@Param("topic") String topic,
		@Param("detailKeywordsJson") String detailKeywordsJson,
		@Param("maxMembers") int maxMembers,
		@Param("startsAt") LocalDate startsAt,
		@Param("endsAt") LocalDate endsAt,
		@Param("updatedAt") Instant updatedAt
	);

	@DeleteProvider(type = StudyGroupMyBatisSqlProvider.class, method = "deleteStudyGroup")
	int deleteStudyGroup(@Param("groupId") UUID groupId, @Param("ownerUserId") UUID ownerUserId, @Param("deletedAt") Instant deletedAt);
}
