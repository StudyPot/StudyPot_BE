package com.studypot.aistudyleader.studygroup.board.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoard;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardComment;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardCommentCursor;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardMembership;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPost;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPostCursor;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPostSummary;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardType;
import com.studypot.aistudyleader.studygroup.board.repository.GroupBoardRepository;
import com.studypot.aistudyleader.studygroup.board.service.GroupBoardService;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(classes = {AiStudyLeaderApplication.class, GroupBoardControllerTest.TestGroupBoardBeans.class})
@AutoConfigureMockMvc
class GroupBoardControllerTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000124001");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000124002");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000124003");
	private static final UUID BOARD_ID = UUID.fromString("018f0000-0000-7000-8000-000000124004");
	private static final UUID POST_ID = UUID.fromString("018f0000-0000-7000-8000-000000124005");
	private static final UUID COMMENT_ID = UUID.fromString("018f0000-0000-7000-8000-000000124006");
	private static final Instant NOW = Instant.parse("2026-06-01T03:00:00Z");
	private static final String BOARDS_PATH = ApiPaths.V1 + "/groups/" + GROUP_ID + "/boards";
	private static final String POSTS_PATH = BOARDS_PATH + "/" + BOARD_ID + "/posts";
	private static final String POST_PATH = ApiPaths.V1 + "/groups/" + GROUP_ID + "/posts/" + POST_ID;
	private static final String COMMENTS_PATH = POST_PATH + "/comments";

	private final MockMvc mockMvc;

	@Autowired
	GroupBoardControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void listBoardsReturnsDefaultBoards() throws Exception {
		mockMvc.perform(get(BOARDS_PATH).with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$[0].boardType").value("NOTICE"))
			.andExpect(jsonPath("$[0].name").value("공지"));
	}

	@Test
	void createPostReturnsCreatedPost() throws Exception {
		mockMvc.perform(post(POSTS_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("board-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title":"질문입니다","content":"JPA 연관관계가 헷갈립니다.","pinned":false}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(POST_ID.toString()))
			.andExpect(jsonPath("$.title").value("질문입니다"))
			.andExpect(jsonPath("$.author.displayName").value("현우"));
	}

	@Test
	void patchPostReturnsValidationProblemForEmptyRequest() throws Exception {
		mockMvc.perform(patch(POST_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("board-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void createCommentReturnsCreatedComment() throws Exception {
		mockMvc.perform(post(COMMENTS_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("board-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"content":"저도 같은 부분이 궁금합니다."}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.content").value("저도 같은 부분이 궁금합니다."));
	}

	@Test
	void createReplyCommentReturnsParentCommentId() throws Exception {
		mockMvc.perform(post(COMMENTS_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("board-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"parentCommentId":"018f0000-0000-7000-8000-000000124006","content":"부모 댓글에 답변합니다."}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.parentCommentId").value(COMMENT_ID.toString()))
			.andExpect(jsonPath("$.content").value("부모 댓글에 답변합니다."));
	}

	private static RequestPostProcessor xsrf(String token) {
		return request -> {
			request.addHeader("X-XSRF-TOKEN", token);
			request.setCookies(new org.springframework.mock.web.MockCookie("XSRF-TOKEN", token));
			return request;
		};
	}

	@TestConfiguration
	static class TestGroupBoardBeans {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}

		@Bean
		@Primary
		GroupBoardRepository groupBoardRepository() {
			return new StaticRepository();
		}

		@Bean
		@Primary
		GroupBoardService groupBoardService(GroupBoardRepository repository, Clock clock) {
			return new GroupBoardService(repository, clock, () -> POST_ID);
		}
	}

	private static final class StaticRepository implements GroupBoardRepository {

		private GroupBoardPost lastInsertedPost;
		private GroupBoardComment lastInsertedComment;
		private final GroupBoardComment parentComment = GroupBoardComment.create(COMMENT_ID, GROUP_ID, POST_ID, MEMBER_ID, "부모 댓글", NOW);

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			return true;
		}

		@Override
		public Optional<GroupBoardMembership> findMembership(UUID groupId, UUID userId) {
			return Optional.of(new GroupBoardMembership(GROUP_ID, MEMBER_ID, USER_ID, "현우", GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE));
		}

		@Override
		public List<GroupBoard> findBoardsByGroupId(UUID groupId) {
			return List.of(GroupBoard.createDefault(BOARD_ID, GROUP_ID, GroupBoardType.NOTICE, 1, NOW));
		}

		@Override
		public void insertDefaultBoards(List<GroupBoard> boards) {
		}

		@Override
		public Optional<GroupBoard> findBoard(UUID groupId, UUID boardId) {
			return Optional.of(GroupBoard.createDefault(BOARD_ID, GROUP_ID, GroupBoardType.NOTICE, 1, NOW));
		}

		@Override
		public boolean existsBoard(UUID boardId) {
			return true;
		}

		@Override
		public boolean insertPost(GroupBoardPost post) {
			lastInsertedPost = post;
			return true;
		}

		@Override
		public List<GroupBoardPostSummary> findPosts(UUID groupId, UUID boardId, GroupBoardPostCursor cursor, int limit) {
			return List.of();
		}

		@Override
		public Optional<GroupBoardPost> findPost(UUID groupId, UUID postId) {
			return Optional.ofNullable(lastInsertedPost)
				.or(() -> Optional.of(GroupBoardPost.create(POST_ID, GROUP_ID, BOARD_ID, MEMBER_ID, "질문입니다", "본문", false, NOW)));
		}

		@Override
		public boolean existsPost(UUID postId) {
			return true;
		}

		@Override
		public boolean updatePost(GroupBoardPost post) {
			lastInsertedPost = post;
			return true;
		}

		@Override
		public boolean softDeletePost(UUID groupId, UUID postId, Instant deletedAt) {
			return true;
		}

		@Override
		public boolean insertComment(GroupBoardComment comment) {
			lastInsertedComment = comment;
			return true;
		}

		@Override
		public List<GroupBoardComment> findComments(UUID groupId, UUID postId, GroupBoardCommentCursor cursor, int limit) {
			return List.of();
		}

		@Override
		public Optional<GroupBoardComment> findComment(UUID groupId, UUID commentId) {
			if (COMMENT_ID.equals(commentId)) {
				return Optional.of(parentComment);
			}
			return Optional.ofNullable(lastInsertedComment);
		}

		@Override
		public boolean existsComment(UUID commentId) {
			return true;
		}

		@Override
		public boolean updateComment(GroupBoardComment comment) {
			lastInsertedComment = comment;
			return true;
		}

		@Override
		public boolean softDeleteComment(UUID groupId, UUID commentId, Instant deletedAt) {
			return true;
		}
	}
}
