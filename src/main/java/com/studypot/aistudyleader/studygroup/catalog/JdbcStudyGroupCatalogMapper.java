package com.studypot.aistudyleader.studygroup.catalog;

import com.studypot.aistudyleader.global.persistence.UuidBinary;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcStudyGroupCatalogMapper implements StudyGroupCatalogMapper {

	private final JdbcTemplate jdbcTemplate;

	JdbcStudyGroupCatalogMapper(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
	}

	@Override
	public StudyGroupCatalogEntry insertStudyGroup(StudyGroupCatalogEntry entry) {
		Objects.requireNonNull(entry, "entry must not be null");
		jdbcTemplate.update(
			StudyGroupCatalogJdbcSql.INSERT_STUDY_GROUP,
			uuid(entry.id()),
			entry.name(),
			entry.topic(),
			entry.status(),
			Date.valueOf(entry.startsAt()),
			Date.valueOf(entry.endsAt()),
			entry.memberCount(),
			entry.averageRating(),
			entry.favorite()
		);
		return entry;
	}

	@Override
	public List<StudyGroupCatalogEntry> searchStudyGroups(String keyword, String status, String sort, int pageSize, String cursor) {
		String normalizedKeyword = text(keyword);
		String normalizedStatus = text(status);
		byte[] cursorId = cursor(cursor);
		return jdbcTemplate.query(
			StudyGroupCatalogJdbcSql.SEARCH_STUDY_GROUPS,
			this::mapEntry,
			normalizedKeyword,
			normalizedKeyword,
			normalizedKeyword,
			normalizedKeyword,
			normalizedStatus,
			normalizedStatus,
			normalizedStatus,
			cursorId,
			cursorId,
			text(sort),
			text(sort),
			pageSize
		);
	}

	@Override
	public Optional<StudyGroupCatalogEntry> findStudyGroupDetail(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		List<StudyGroupCatalogEntry> entries = jdbcTemplate.query(
			StudyGroupCatalogJdbcSql.SELECT_DETAIL,
			this::mapEntry,
			uuid(groupId)
		);
		return entries.stream().findFirst();
	}

	@Override
	public StudyGroupCatalogEntry updateStudyGroup(UUID groupId, StudyGroupCatalogCommand command) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(command, "command must not be null");
		jdbcTemplate.update(
			StudyGroupCatalogJdbcSql.UPDATE_STUDY_GROUP,
			command.name().strip(),
			command.topic().strip(),
			command.status().strip().toUpperCase(),
			Date.valueOf(command.startsAt()),
			Date.valueOf(command.endsAt()),
			command.favorite(),
			uuid(groupId)
		);
		return findStudyGroupDetail(groupId).orElseThrow();
	}

	@Override
	public boolean deleteStudyGroup(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return jdbcTemplate.update(StudyGroupCatalogJdbcSql.SOFT_DELETE_STUDY_GROUP, uuid(groupId)) > 0;
	}

	private StudyGroupCatalogEntry mapEntry(ResultSet resultSet, int rowNumber) throws SQLException {
		return new StudyGroupCatalogEntry(
			uuid(resultSet, "id"),
			resultSet.getString("name"),
			resultSet.getString("topic"),
			resultSet.getString("status"),
			resultSet.getDate("starts_at").toLocalDate(),
			resultSet.getDate("ends_at").toLocalDate(),
			resultSet.getInt("member_count"),
			resultSet.getDouble("average_rating"),
			resultSet.getBoolean("favorite")
		);
	}

	private static String text(String value) {
		return value == null ? "" : value.strip();
	}

	private static byte[] cursor(String cursor) {
		String normalizedCursor = text(cursor);
		if (normalizedCursor.isBlank()) {
			return null;
		}
		return uuid(UUID.fromString(normalizedCursor));
	}

	private static byte[] uuid(UUID uuid) {
		return UuidBinary.toBytes(uuid);
	}

	private static UUID uuid(ResultSet resultSet, String column) throws SQLException {
		return UuidBinary.fromBytes(resultSet.getBytes(column));
	}
}
