package com.studypot.aistudyleader.studygroup.board.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoard;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardComment;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPost;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPostCursor;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardType;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class JdbcGroupBoardRepositoryTest {

	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000125001");
	private static final UUID BOARD_ID = UUID.fromString("018f0000-0000-7000-8000-000000125002");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000125003");
	private static final UUID POST_ID = UUID.fromString("018f0000-0000-7000-8000-000000125004");
	private static final UUID COMMENT_ID = UUID.fromString("018f0000-0000-7000-8000-000000125005");
	private static final UUID PARENT_COMMENT_ID = UUID.fromString("018f0000-0000-7000-8000-000000125006");
	private static final Instant NOW = Instant.parse("2026-06-01T04:00:00Z");

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final JdbcGroupBoardRepository repository = new JdbcGroupBoardRepository(jdbcTemplate, new ObjectMapper());

	@Test
	void insertDefaultBoardsUsesInsertIgnoreForIdempotency() {
		GroupBoard board = GroupBoard.createDefault(BOARD_ID, GROUP_ID, GroupBoardType.NOTICE, 1, NOW);
		when(jdbcTemplate.update(eq(GroupBoardJdbcSql.INSERT_DEFAULT_BOARD), any(Object[].class))).thenReturn(1);

		repository.insertDefaultBoards(List.of(board));

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(GroupBoardJdbcSql.INSERT_DEFAULT_BOARD), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(BOARD_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat(args.getValue()[2]).isEqualTo("NOTICE");
		assertThat(args.getValue()[3]).isEqualTo("공지");
	}

	@Test
	void findPostsUsesPinnedCursorAndPageLimit() {
		GroupBoardPostCursor cursor = new GroupBoardPostCursor(true, NOW, POST_ID);
		when(jdbcTemplate.query(eq(GroupBoardJdbcSql.SELECT_POSTS), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of());

		repository.findPosts(GROUP_ID, BOARD_ID, cursor, 21);

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(GroupBoardJdbcSql.SELECT_POSTS), any(org.springframework.jdbc.core.RowMapper.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(BOARD_ID));
		assertThat(args.getValue()[2]).isEqualTo(true);
		assertThat(args.getValue()[3]).isEqualTo(true);
		assertThat(args.getValue()[4]).isEqualTo(true);
		assertThat(args.getValue()[5]).isEqualTo(Timestamp.from(NOW));
		assertThat(args.getValue()[9]).isEqualTo(21);
	}

	@Test
	void insertPostPersistsAuthorAndPinnedState() {
		GroupBoardPost post = GroupBoardPost.create(POST_ID, GROUP_ID, BOARD_ID, MEMBER_ID, "질문", "본문", true, NOW);
		when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

		assertThat(repository.insertPost(post)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(GroupBoardJdbcSql.INSERT_POST), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(POST_ID));
		assertThat((byte[]) args.getValue()[3]).containsExactly(UuidBinary.toBytes(MEMBER_ID));
		assertThat(args.getValue()[6]).isEqualTo(true);
	}

	@Test
	void insertCommentBindsNullParentCommentIdForTopLevelComments() {
		GroupBoardComment comment = GroupBoardComment.create(COMMENT_ID, GROUP_ID, POST_ID, MEMBER_ID, "댓글입니다.", NOW);
		when(jdbcTemplate.update(eq(GroupBoardJdbcSql.INSERT_COMMENT), any(Object[].class))).thenReturn(1);

		assertThat(repository.insertComment(comment)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(GroupBoardJdbcSql.INSERT_COMMENT), args.capture());
		assertThat(args.getValue()[3]).isNull();
		assertThat((byte[]) args.getValue()[4]).containsExactly(UuidBinary.toBytes(MEMBER_ID));
		assertThat(args.getValue()[5]).isEqualTo("댓글입니다.");
	}

	@Test
	void insertCommentPersistsParentCommentIdForReplies() {
		GroupBoardComment comment = GroupBoardComment.create(
			COMMENT_ID,
			GROUP_ID,
			POST_ID,
			PARENT_COMMENT_ID,
			MEMBER_ID,
			null,
			null,
			"대댓글입니다.",
			NOW
		);
		when(jdbcTemplate.update(eq(GroupBoardJdbcSql.INSERT_COMMENT), any(Object[].class))).thenReturn(1);

		assertThat(repository.insertComment(comment)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(GroupBoardJdbcSql.INSERT_COMMENT), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(COMMENT_ID));
		assertThat((byte[]) args.getValue()[3]).containsExactly(UuidBinary.toBytes(PARENT_COMMENT_ID));
		assertThat((byte[]) args.getValue()[4]).containsExactly(UuidBinary.toBytes(MEMBER_ID));
		assertThat(args.getValue()[5]).isEqualTo("대댓글입니다.");
	}

	@Test
	void findCommentMapsParentCommentIdForReplies() throws Exception {
		ResultSet resultSet = mock(ResultSet.class);
		when(resultSet.getBytes("id")).thenReturn(UuidBinary.toBytes(COMMENT_ID));
		when(resultSet.getBytes("group_id")).thenReturn(UuidBinary.toBytes(GROUP_ID));
		when(resultSet.getBytes("post_id")).thenReturn(UuidBinary.toBytes(POST_ID));
		when(resultSet.getBytes("parent_comment_id")).thenReturn(UuidBinary.toBytes(PARENT_COMMENT_ID));
		when(resultSet.getBytes("author_member_id")).thenReturn(UuidBinary.toBytes(MEMBER_ID));
		when(resultSet.getBytes("author_user_id")).thenReturn(null);
		when(resultSet.getString("author_display_name")).thenReturn(null);
		when(resultSet.getString("content")).thenReturn("대댓글입니다.");
		when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(NOW));
		when(resultSet.getTimestamp("updated_at")).thenReturn(Timestamp.from(NOW));
		when(resultSet.getTimestamp("deleted_at")).thenReturn(null);
		when(jdbcTemplate.query(eq(GroupBoardJdbcSql.SELECT_COMMENT), any(RowMapper.class), any(Object[].class)))
			.thenAnswer(invocation -> {
				@SuppressWarnings("unchecked")
				RowMapper<GroupBoardComment> mapper = invocation.getArgument(1);
				return List.of(mapper.mapRow(resultSet, 0));
			});

		Optional<GroupBoardComment> comment = repository.findComment(GROUP_ID, COMMENT_ID);

		assertThat(comment).hasValueSatisfying(found -> assertThat(found.parentCommentId()).isEqualTo(PARENT_COMMENT_ID));
	}

	@Test
	void softDeleteCommentDeletesTargetCommentAndDirectReplies() {
		when(jdbcTemplate.update(eq(GroupBoardJdbcSql.SOFT_DELETE_COMMENT_THREAD), any(Object[].class))).thenReturn(2);

		assertThat(repository.softDeleteComment(GROUP_ID, COMMENT_ID, NOW)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(GroupBoardJdbcSql.SOFT_DELETE_COMMENT_THREAD), args.capture());
		assertThat(args.getValue()[0]).isEqualTo(Timestamp.from(NOW));
		assertThat(args.getValue()[1]).isEqualTo(Timestamp.from(NOW));
		assertThat((byte[]) args.getValue()[2]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) args.getValue()[3]).containsExactly(UuidBinary.toBytes(COMMENT_ID));
		assertThat((byte[]) args.getValue()[4]).containsExactly(UuidBinary.toBytes(COMMENT_ID));
	}
}
