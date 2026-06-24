package com.studypot.aistudyleader.studygroup.board.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoard;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardComment;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardCommentCursor;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardMembership;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPost;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPostCursor;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPostSummary;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPostSort;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardType;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcGroupBoardRepository implements GroupBoardRepository {

	private final JdbcTemplate jdbcTemplate;

	JdbcGroupBoardRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
		Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public boolean existsStudyGroup(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(GroupBoardJdbcSql.EXISTS_STUDY_GROUP, Boolean.class, uuid(groupId)));
	}

	@Override
	public Optional<GroupBoardMembership> findMembership(UUID groupId, UUID userId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		return queryOne(GroupBoardJdbcSql.SELECT_MEMBERSHIP, this::mapMembership, uuid(groupId), uuid(userId));
	}

	@Override
	public List<GroupBoard> findBoardsByGroupId(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return jdbcTemplate.query(GroupBoardJdbcSql.SELECT_BOARDS_BY_GROUP_ID, this::mapBoard, uuid(groupId));
	}

	@Override
	public void insertDefaultBoards(List<GroupBoard> boards) {
		Objects.requireNonNull(boards, "boards must not be null");
		for (GroupBoard board : boards) {
			jdbcTemplate.update(
				GroupBoardJdbcSql.INSERT_DEFAULT_BOARD,
				uuid(board.id()),
				uuid(board.groupId()),
				board.boardType().name(),
				board.name(),
				board.description(),
				board.displayOrder(),
				board.defaultBoard(),
				timestamp(board.createdAt()),
				timestamp(board.updatedAt())
			);
		}
	}

	@Override
	public Optional<GroupBoard> findBoard(UUID groupId, UUID boardId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(boardId, "boardId must not be null");
		return queryOne(GroupBoardJdbcSql.SELECT_BOARD, this::mapBoard, uuid(groupId), uuid(boardId));
	}

	@Override
	public boolean existsBoard(UUID boardId) {
		Objects.requireNonNull(boardId, "boardId must not be null");
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(GroupBoardJdbcSql.EXISTS_BOARD, Boolean.class, uuid(boardId)));
	}

	@Override
	public boolean insertPost(GroupBoardPost post) {
		Objects.requireNonNull(post, "post must not be null");
		return jdbcTemplate.update(
			GroupBoardJdbcSql.INSERT_POST,
			uuid(post.id()),
			uuid(post.groupId()),
			uuid(post.boardId()),
			uuid(post.authorMemberId()),
			post.authorDisplayNameOverride(),
			post.title(),
			post.content(),
			post.pinned(),
			timestamp(post.createdAt()),
			timestamp(post.updatedAt())
		) > 0;
	}

	@Override
	public List<GroupBoardPostSummary> findPosts(
		UUID groupId, UUID boardId, GroupBoardPostCursor cursor, GroupBoardPostSort sort, int limit) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(boardId, "boardId must not be null");
		Objects.requireNonNull(sort, "sort must not be null");
		Boolean cursorPinned = cursor == null ? null : cursor.pinned();
		Timestamp cursorCreatedAt = cursor == null ? null : timestamp(cursor.createdAt());
		byte[] cursorId = cursor == null ? null : uuid(cursor.id());
		String sql = GroupBoardJdbcSql.SELECT_POSTS + "\n" + sort.orderByClause() + "\nlimit ?";
		return jdbcTemplate.query(
			sql,
			this::mapPostSummary,
			uuid(groupId),
			uuid(boardId),
			cursorPinned,
			cursorPinned,
			cursorPinned,
			cursorCreatedAt,
			cursorPinned,
			cursorCreatedAt,
			cursorId,
			limit
		);
	}

	@Override
	public List<GroupBoardPostSummary> findAllPosts(
		UUID groupId, GroupBoardPostCursor cursor, GroupBoardPostSort sort, int limit) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(sort, "sort must not be null");
		Boolean cursorPinned = cursor == null ? null : cursor.pinned();
		Timestamp cursorCreatedAt = cursor == null ? null : timestamp(cursor.createdAt());
		byte[] cursorId = cursor == null ? null : uuid(cursor.id());
		String sql = GroupBoardJdbcSql.SELECT_ALL_POSTS + "\n" + sort.orderByClause() + "\nlimit ?";
		return jdbcTemplate.query(
			sql,
			this::mapPostSummary,
			uuid(groupId),
			cursorPinned,
			cursorPinned,
			cursorPinned,
			cursorCreatedAt,
			cursorPinned,
			cursorCreatedAt,
			cursorId,
			limit
		);
	}

	@Override
	public Optional<GroupBoardPost> findPost(UUID groupId, UUID postId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(postId, "postId must not be null");
		return queryOne(GroupBoardJdbcSql.SELECT_POST, this::mapPost, uuid(groupId), uuid(postId));
	}

	@Override
	public boolean existsPost(UUID postId) {
		Objects.requireNonNull(postId, "postId must not be null");
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(GroupBoardJdbcSql.EXISTS_POST, Boolean.class, uuid(postId)));
	}

	@Override
	public boolean updatePost(GroupBoardPost post) {
		Objects.requireNonNull(post, "post must not be null");
		return jdbcTemplate.update(
			GroupBoardJdbcSql.UPDATE_POST,
			post.title(),
			post.content(),
			post.pinned(),
			timestamp(post.updatedAt()),
			uuid(post.groupId()),
			uuid(post.id())
		) > 0;
	}

	@Override
	public boolean softDeletePost(UUID groupId, UUID postId, Instant deletedAt) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(postId, "postId must not be null");
		Objects.requireNonNull(deletedAt, "deletedAt must not be null");
		return jdbcTemplate.update(
			GroupBoardJdbcSql.SOFT_DELETE_POST,
			timestamp(deletedAt),
			timestamp(deletedAt),
			uuid(groupId),
			uuid(postId)
		) > 0;
	}

	@Override
	public boolean insertComment(GroupBoardComment comment) {
		Objects.requireNonNull(comment, "comment must not be null");
		return jdbcTemplate.update(
			GroupBoardJdbcSql.INSERT_COMMENT,
			uuid(comment.id()),
			uuid(comment.groupId()),
			uuid(comment.postId()),
			uuid(comment.authorMemberId()),
			comment.content(),
			timestamp(comment.createdAt()),
			timestamp(comment.updatedAt())
		) > 0;
	}

	@Override
	public List<GroupBoardComment> findComments(UUID groupId, UUID postId, GroupBoardCommentCursor cursor, int limit) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(postId, "postId must not be null");
		Timestamp cursorCreatedAt = cursor == null ? null : timestamp(cursor.createdAt());
		byte[] cursorId = cursor == null ? null : uuid(cursor.id());
		return jdbcTemplate.query(
			GroupBoardJdbcSql.SELECT_COMMENTS,
			this::mapComment,
			uuid(groupId),
			uuid(postId),
			cursorCreatedAt,
			cursorCreatedAt,
			cursorCreatedAt,
			cursorId,
			limit
		);
	}

	@Override
	public Optional<GroupBoardComment> findComment(UUID groupId, UUID commentId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(commentId, "commentId must not be null");
		return queryOne(GroupBoardJdbcSql.SELECT_COMMENT, this::mapComment, uuid(groupId), uuid(commentId));
	}

	@Override
	public boolean existsComment(UUID commentId) {
		Objects.requireNonNull(commentId, "commentId must not be null");
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(GroupBoardJdbcSql.EXISTS_COMMENT, Boolean.class, uuid(commentId)));
	}

	@Override
	public boolean updateComment(GroupBoardComment comment) {
		Objects.requireNonNull(comment, "comment must not be null");
		return jdbcTemplate.update(
			GroupBoardJdbcSql.UPDATE_COMMENT,
			comment.content(),
			timestamp(comment.updatedAt()),
			uuid(comment.groupId()),
			uuid(comment.id())
		) > 0;
	}

	@Override
	public boolean softDeleteComment(UUID groupId, UUID commentId, Instant deletedAt) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(commentId, "commentId must not be null");
		Objects.requireNonNull(deletedAt, "deletedAt must not be null");
		return jdbcTemplate.update(
			GroupBoardJdbcSql.SOFT_DELETE_COMMENT,
			timestamp(deletedAt),
			timestamp(deletedAt),
			uuid(groupId),
			uuid(commentId)
		) > 0;
	}

	private GroupBoardMembership mapMembership(ResultSet resultSet, int rowNumber) throws SQLException {
		return new GroupBoardMembership(
			requiredUuid(resultSet, "group_id"),
			requiredUuid(resultSet, "member_id"),
			requiredUuid(resultSet, "user_id"),
			resultSet.getString("display_name"),
			requiredEnum(resultSet, "permission", GroupMemberPermission.class),
			requiredEnum(resultSet, "member_status", GroupMemberStatus.class)
		);
	}

	private GroupBoard mapBoard(ResultSet resultSet, int rowNumber) throws SQLException {
		return new GroupBoard(
			requiredUuid(resultSet, "id"),
			requiredUuid(resultSet, "group_id"),
			requiredEnum(resultSet, "board_type", GroupBoardType.class),
			requiredString(resultSet, "name"),
			resultSet.getString("description"),
			resultSet.getInt("display_order"),
			resultSet.getBoolean("is_default"),
			requiredInstant(resultSet, "created_at"),
			requiredInstant(resultSet, "updated_at"),
			instant(resultSet.getTimestamp("deleted_at"))
		);
	}

	private GroupBoardPostSummary mapPostSummary(ResultSet resultSet, int rowNumber) throws SQLException {
		return new GroupBoardPostSummary(
			requiredUuid(resultSet, "id"),
			requiredUuid(resultSet, "group_id"),
			requiredUuid(resultSet, "board_id"),
			requiredUuid(resultSet, "author_member_id"),
			uuid(resultSet.getBytes("author_user_id")),
			resultSet.getString("author_display_name"),
			requiredString(resultSet, "title"),
			resultSet.getString("content_preview"),
			resultSet.getBoolean("is_pinned"),
			resultSet.getInt("comment_count"),
			requiredInstant(resultSet, "created_at"),
			requiredInstant(resultSet, "updated_at")
		);
	}

	private GroupBoardPost mapPost(ResultSet resultSet, int rowNumber) throws SQLException {
		return new GroupBoardPost(
			requiredUuid(resultSet, "id"),
			requiredUuid(resultSet, "group_id"),
			requiredUuid(resultSet, "board_id"),
			requiredUuid(resultSet, "author_member_id"),
			uuid(resultSet.getBytes("author_user_id")),
			resultSet.getString("author_display_name"),
			null,
			requiredString(resultSet, "title"),
			requiredString(resultSet, "content"),
			resultSet.getBoolean("is_pinned"),
			requiredInstant(resultSet, "created_at"),
			requiredInstant(resultSet, "updated_at"),
			instant(resultSet.getTimestamp("deleted_at"))
		);
	}

	private GroupBoardComment mapComment(ResultSet resultSet, int rowNumber) throws SQLException {
		return new GroupBoardComment(
			requiredUuid(resultSet, "id"),
			requiredUuid(resultSet, "group_id"),
			requiredUuid(resultSet, "post_id"),
			requiredUuid(resultSet, "author_member_id"),
			uuid(resultSet.getBytes("author_user_id")),
			resultSet.getString("author_display_name"),
			requiredString(resultSet, "content"),
			requiredInstant(resultSet, "created_at"),
			requiredInstant(resultSet, "updated_at"),
			instant(resultSet.getTimestamp("deleted_at"))
		);
	}

	private <T> Optional<T> queryOne(String sql, ThrowingRowMapper<T> mapper, Object... args) {
		List<T> results = jdbcTemplate.query(sql, (resultSet, rowNumber) -> mapper.map(resultSet, rowNumber), args);
		return results.stream().findFirst();
	}

	private static byte[] uuid(UUID uuid) {
		return UuidBinary.toBytes(uuid);
	}

	private static UUID uuid(byte[] bytes) {
		return bytes == null ? null : UuidBinary.fromBytes(bytes);
	}

	private static UUID requiredUuid(ResultSet resultSet, String columnName) throws SQLException {
		UUID value = uuid(resultSet.getBytes(columnName));
		if (value == null) {
			throw new SQLException(columnName + " must not be null.");
		}
		return value;
	}

	private static String requiredString(ResultSet resultSet, String columnName) throws SQLException {
		String value = resultSet.getString(columnName);
		if (value == null) {
			throw new SQLException(columnName + " must not be null.");
		}
		return value;
	}

	private static <E extends Enum<E>> E requiredEnum(ResultSet resultSet, String columnName, Class<E> enumType) throws SQLException {
		String value = requiredString(resultSet, columnName);
		try {
			return Enum.valueOf(enumType, value);
		} catch (IllegalArgumentException exception) {
			throw new GroupBoardPersistenceException(columnName + " contains invalid enum value: " + value, exception);
		}
	}

	private static Timestamp timestamp(Instant instant) {
		return instant == null ? null : Timestamp.from(instant);
	}

	private static Instant instant(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toInstant();
	}

	private static Instant requiredInstant(ResultSet resultSet, String columnName) throws SQLException {
		Instant value = instant(resultSet.getTimestamp(columnName));
		if (value == null) {
			throw new SQLException(columnName + " must not be null.");
		}
		return value;
	}

	@FunctionalInterface
	private interface ThrowingRowMapper<T> {

		T map(ResultSet resultSet, int rowNumber) throws SQLException;
	}
}
