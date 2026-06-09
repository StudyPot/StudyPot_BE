package com.studypot.aistudyleader.studygroup.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;

@Mapper
public interface StudyGroupCatalogMapper {

	@InsertProvider(type = StudyGroupCatalogSqlProvider.class, method = "insertStudyGroup")
	StudyGroupCatalogEntry insertStudyGroup(StudyGroupCatalogEntry entry);

	@SelectProvider(type = StudyGroupCatalogSqlProvider.class, method = "searchStudyGroups")
	List<StudyGroupCatalogEntry> searchStudyGroups(
		@Param("keyword") String keyword,
		@Param("status") String status,
		@Param("sort") String sort,
		@Param("pageSize") int pageSize,
		@Param("cursor") String cursor
	);

	@SelectProvider(type = StudyGroupCatalogSqlProvider.class, method = "findStudyGroupDetail")
	Optional<StudyGroupCatalogEntry> findStudyGroupDetail(@Param("groupId") UUID groupId);

	@UpdateProvider(type = StudyGroupCatalogSqlProvider.class, method = "updateStudyGroup")
	StudyGroupCatalogEntry updateStudyGroup(@Param("groupId") UUID groupId, @Param("command") StudyGroupCatalogCommand command);

	@DeleteProvider(type = StudyGroupCatalogSqlProvider.class, method = "deleteStudyGroup")
	boolean deleteStudyGroup(@Param("groupId") UUID groupId);
}
