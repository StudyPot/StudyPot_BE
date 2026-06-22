package com.studypot.aistudyleader.bookmark.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.bookmark.domain.BookmarkedGroup;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcBookmarkRepository implements BookmarkRepository {

	private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
	};

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	JdbcBookmarkRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public boolean existsStudyGroup(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(BookmarkJdbcSql.EXISTS_STUDY_GROUP, Boolean.class, uuid(groupId)));
	}

	@Override
	public Optional<Boolean> findBookmarkActive(UUID userId, UUID groupId) {
		Objects.requireNonNull(userId, "userId must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		return jdbcTemplate.query(
			BookmarkJdbcSql.SELECT_BOOKMARK_ACTIVE,
			(resultSet, rowNumber) -> resultSet.getBoolean("active"),
			uuid(userId),
			uuid(groupId)
		).stream().findFirst();
	}

	@Override
	public void insertBookmark(UUID id, UUID userId, UUID groupId, Instant now) {
		jdbcTemplate.update(
			BookmarkJdbcSql.INSERT_BOOKMARK,
			uuid(id),
			uuid(userId),
			uuid(groupId),
			Timestamp.from(now),
			Timestamp.from(now)
		);
	}

	@Override
	public void reactivateBookmark(UUID userId, UUID groupId, Instant now) {
		jdbcTemplate.update(
			BookmarkJdbcSql.REACTIVATE_BOOKMARK,
			Timestamp.from(now),
			Timestamp.from(now),
			uuid(userId),
			uuid(groupId)
		);
	}

	@Override
	public void softDeleteBookmark(UUID userId, UUID groupId, Instant now) {
		jdbcTemplate.update(
			BookmarkJdbcSql.SOFT_DELETE_BOOKMARK,
			Timestamp.from(now),
			Timestamp.from(now),
			uuid(userId),
			uuid(groupId)
		);
	}

	@Override
	public List<BookmarkedGroup> findMyBookmarks(UUID userId) {
		Objects.requireNonNull(userId, "userId must not be null");
		return jdbcTemplate.query(BookmarkJdbcSql.SELECT_MY_BOOKMARKS, this::mapBookmarkedGroup, uuid(userId));
	}

	private BookmarkedGroup mapBookmarkedGroup(ResultSet resultSet, int rowNumber) throws SQLException {
		StudyGroup group = StudyGroup.rehydrate(
			UuidBinary.fromBytes(resultSet.getBytes("id")),
			UuidBinary.fromBytes(resultSet.getBytes("created_by")),
			resultSet.getString("name"),
			resultSet.getString("topic"),
			readDetailKeywords(resultSet.getString("detail_keywords")),
			StudyGroupStatus.valueOf(resultSet.getString("status")),
			resultSet.getInt("max_members"),
			resultSet.getBoolean("is_public"),
			resultSet.getString("invite_code"),
			resultSet.getDate("starts_at").toLocalDate(),
			resultSet.getDate("ends_at").toLocalDate(),
			resultSet.getString("description"),
			instant(resultSet.getTimestamp("onboarding_started_at")),
			instant(resultSet.getTimestamp("started_at")),
			instant(resultSet.getTimestamp("created_at")),
			instant(resultSet.getTimestamp("updated_at"))
		);
		return new BookmarkedGroup(group, instant(resultSet.getTimestamp("bookmarked_at")));
	}

	private List<String> readDetailKeywords(String value) {
		if (value == null || value.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(value, STRING_LIST);
		} catch (JsonProcessingException exception) {
			throw new BookmarkPersistenceException("bookmark detail keywords could not be deserialized.", exception);
		}
	}

	private static Instant instant(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toInstant();
	}

	private static byte[] uuid(UUID uuid) {
		return UuidBinary.toBytes(uuid);
	}
}
