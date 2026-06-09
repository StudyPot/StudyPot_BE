package com.studypot.aistudyleader.studygroup.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.global.api.CursorPageResponse;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoard;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardComment;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardCommentCursor;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardMembership;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPost;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPostCursor;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPostSummary;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardType;
import com.studypot.aistudyleader.studygroup.board.repository.GroupBoardRepository;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GroupBoardServiceTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000123001");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000123002");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000123003");
	private static final UUID BOARD_ID = UUID.fromString("018f0000-0000-7000-8000-000000123004");
	private static final UUID POST_ID = UUID.fromString("018f0000-0000-7000-8000-000000123005");
	private static final UUID COMMENT_ID = UUID.fromString("018f0000-0000-7000-8000-000000123006");
	private static final UUID OTHER_MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000123007");
	private static final Instant NOW = Instant.parse("2026-06-01T02:10:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Test
	void listBoardsInitializesDefaultBoardsForActiveMember() {
		CapturingRepository repository = new CapturingRepository();
		GroupBoardService service = service(repository,
			UUID.fromString("018f0000-0000-7000-8000-000000123101"),
			UUID.fromString("018f0000-0000-7000-8000-000000123102"),
			UUID.fromString("018f0000-0000-7000-8000-000000123103"),
			UUID.fromString("018f0000-0000-7000-8000-000000123104")
		);

		List<GroupBoard> boards = service.listBoards(new ListGroupBoardsQuery(USER_ID, GROUP_ID));

		assertThat(repository.insertedBoards)
			.extracting(GroupBoard::boardType)
			.containsExactly(GroupBoardType.NOTICE, GroupBoardType.QUESTION, GroupBoardType.RESOURCE, GroupBoardType.RETROSPECTIVE);
		assertThat(boards).hasSize(4);
	}

	@Test
	void createPostRejectsPendingMemberBeforeInsert() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = new GroupBoardMembership(GROUP_ID, MEMBER_ID, GroupMemberPermission.MEMBER, GroupMemberStatus.PENDING_ONBOARDING);
		GroupBoardService service = service(repository, POST_ID);

		assertThatThrownBy(() -> service.createPost(new CreateGroupBoardPostCommand(
				USER_ID,
				GROUP_ID,
				BOARD_ID,
				"질문",
				"JPA 연관관계 질문입니다.",
				false
			)))
			.isInstanceOf(GroupBoardAccessDeniedException.class)
			.hasMessage("active group membership is required for group board access.");
		assertThat(repository.insertedPost).isNull();
	}

	@Test
	void createPostAllowsOwnerPinnedPostAndReturnsStoredPost() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = new GroupBoardMembership(GROUP_ID, MEMBER_ID, GroupMemberPermission.OWNER, GroupMemberStatus.ACTIVE);
		repository.boards = List.of(board());
		GroupBoardService service = service(repository, POST_ID);

		GroupBoardPost post = service.createPost(new CreateGroupBoardPostCommand(
			USER_ID,
			GROUP_ID,
			BOARD_ID,
			"공지",
			"이번 주 회고는 금요일까지 제출해주세요.",
			true
		));

		assertThat(post.id()).isEqualTo(POST_ID);
		assertThat(post.pinned()).isTrue();
		assertThat(repository.insertedPost.authorMemberId()).isEqualTo(MEMBER_ID);
	}

	@Test
	void updatePostRejectsNonAuthorContentRewriteButAllowsOwnerPinChange() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = new GroupBoardMembership(GROUP_ID, MEMBER_ID, GroupMemberPermission.OWNER, GroupMemberStatus.ACTIVE);
		repository.post = post(OTHER_MEMBER_ID, false);
		GroupBoardService service = service(repository);

		assertThatThrownBy(() -> service.updatePost(new UpdateGroupBoardPostCommand(
				USER_ID,
				GROUP_ID,
				POST_ID,
				"남의 글 제목",
				null,
				null
			)))
			.isInstanceOf(GroupBoardAccessDeniedException.class)
			.hasMessage("only the post author can update post title or content.");

		GroupBoardPost updated = service.updatePost(new UpdateGroupBoardPostCommand(
			USER_ID,
			GROUP_ID,
			POST_ID,
			null,
			null,
			true
		));

		assertThat(updated.pinned()).isTrue();
	}

	@Test
	void listPostsReturnsCursorPageAndRejectsInvalidCursor() {
		CapturingRepository repository = new CapturingRepository();
		repository.boards = List.of(board());
		repository.postSummaries = List.of(
			postSummary(POST_ID, NOW.plusSeconds(2)),
			postSummary(UUID.fromString("018f0000-0000-7000-8000-000000123008"), NOW.plusSeconds(1))
		);
		GroupBoardService service = service(repository);

		CursorPageResponse<GroupBoardPostSummary> page = service.listPosts(new ListGroupBoardPostsQuery(
			USER_ID,
			GROUP_ID,
			BOARD_ID,
			null,
			1
		));

		assertThat(page.items()).hasSize(1);
		assertThat(page.pageInfo().hasNext()).isTrue();
		assertThat(page.pageInfo().nextCursor()).isNotBlank();
		assertThat(repository.lastPostLimit).isEqualTo(2);
		assertThatThrownBy(() -> service.listPosts(new ListGroupBoardPostsQuery(
				USER_ID,
				GROUP_ID,
				BOARD_ID,
				"not-a-cursor",
				20
			)))
			.isInstanceOf(InvalidGroupBoardRequestException.class)
			.hasMessage("cursor is invalid.");

		String invalidBooleanCursor = Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString(("maybe|" + NOW + "|" + POST_ID).getBytes(StandardCharsets.UTF_8));
		assertThatThrownBy(() -> service.listPosts(new ListGroupBoardPostsQuery(
				USER_ID,
				GROUP_ID,
				BOARD_ID,
				invalidBooleanCursor,
				20
			)))
			.isInstanceOf(InvalidGroupBoardRequestException.class)
			.hasMessage("cursor is invalid.");
	}

	@Test
	void createCommentRequiresExistingPostBeforeInsert() {
		CapturingRepository repository = new CapturingRepository();
		repository.post = null;
		GroupBoardService service = service(repository, COMMENT_ID);

		assertThatThrownBy(() -> service.createComment(new CreateGroupBoardCommentCommand(
				USER_ID,
				GROUP_ID,
				POST_ID,
				"참고 자료 확인했습니다."
			)))
			.isInstanceOf(GroupBoardNotFoundException.class)
			.hasMessage("group board post was not found.");
		assertThat(repository.insertedComment).isNull();
	}

	@Test
	void createCommentStoresAuthorAndPostAssociationAfterMembershipCheck() {
		CapturingRepository repository = new CapturingRepository();
		GroupBoardService service = service(repository, COMMENT_ID);

		GroupBoardComment comment = service.createComment(new CreateGroupBoardCommentCommand(
			USER_ID,
			GROUP_ID,
			POST_ID,
			"참고 자료 확인했습니다."
		));

		assertThat(comment.id()).isEqualTo(COMMENT_ID);
		assertThat(repository.insertedComment.groupId()).isEqualTo(GROUP_ID);
		assertThat(repository.insertedComment.postId()).isEqualTo(POST_ID);
		assertThat(repository.insertedComment.authorMemberId()).isEqualTo(MEMBER_ID);
	}

	@Test
	void listCommentsReturnsCursorPageAndRejectsInvalidCursor() {
		CapturingRepository repository = new CapturingRepository();
		repository.comments = List.of(
			comment(COMMENT_ID, MEMBER_ID, NOW.plusSeconds(1)),
			comment(UUID.fromString("018f0000-0000-7000-8000-000000123009"), OTHER_MEMBER_ID, NOW.plusSeconds(2))
		);
		GroupBoardService service = service(repository);

		CursorPageResponse<GroupBoardComment> page = service.listComments(new ListGroupBoardCommentsQuery(
			USER_ID,
			GROUP_ID,
			POST_ID,
			null,
			1
		));

		assertThat(page.items()).hasSize(1);
		assertThat(page.pageInfo().hasNext()).isTrue();
		assertThat(page.pageInfo().nextCursor()).isNotBlank();
		assertThat(repository.lastCommentLimit).isEqualTo(2);
		assertThatThrownBy(() -> service.listComments(new ListGroupBoardCommentsQuery(
				USER_ID,
				GROUP_ID,
				POST_ID,
				"not-a-cursor",
				20
			)))
			.isInstanceOf(InvalidGroupBoardRequestException.class)
			.hasMessage("cursor is invalid.");
	}

	@Test
	void updateCommentAllowsOnlyAuthorContentEdit() {
		CapturingRepository repository = new CapturingRepository();
		repository.comment = comment(OTHER_MEMBER_ID);
		GroupBoardService service = service(repository);

		assertThatThrownBy(() -> service.updateComment(new UpdateGroupBoardCommentCommand(
				USER_ID,
				GROUP_ID,
				COMMENT_ID,
				"수정"
			)))
			.isInstanceOf(GroupBoardAccessDeniedException.class)
			.hasMessage("only the comment author can update comment content.");
	}

	@Test
	void updateCommentRejectsBlankContent() {
		CapturingRepository repository = new CapturingRepository();
		GroupBoardService service = service(repository);

		assertThatThrownBy(() -> service.updateComment(new UpdateGroupBoardCommentCommand(
				USER_ID,
				GROUP_ID,
				COMMENT_ID,
				"  "
			)))
			.isInstanceOf(InvalidGroupBoardRequestException.class)
			.hasMessage("content must not be blank.");
	}

	@Test
	void deleteCommentAllowsAuthorOrOwnerAndRejectsOtherMembers() {
		CapturingRepository memberRepository = new CapturingRepository();
		memberRepository.comment = comment(OTHER_MEMBER_ID);
		GroupBoardService memberService = service(memberRepository);

		assertThatThrownBy(() -> memberService.deleteComment(new DeleteGroupBoardCommentCommand(
				USER_ID,
				GROUP_ID,
				COMMENT_ID
			)))
			.isInstanceOf(GroupBoardAccessDeniedException.class)
			.hasMessage("only the comment author or study group owner can delete board comments.");
		assertThat(memberRepository.comment).isNotNull();

		CapturingRepository ownerRepository = new CapturingRepository();
		ownerRepository.membership = new GroupBoardMembership(GROUP_ID, MEMBER_ID, GroupMemberPermission.OWNER, GroupMemberStatus.ACTIVE);
		ownerRepository.comment = comment(OTHER_MEMBER_ID);
		GroupBoardService ownerService = service(ownerRepository);

		ownerService.deleteComment(new DeleteGroupBoardCommentCommand(USER_ID, GROUP_ID, COMMENT_ID));

		assertThat(ownerRepository.comment).isNull();
		assertThat(ownerRepository.lastSoftDeletedCommentAt).isEqualTo(NOW);

		CapturingRepository authorRepository = new CapturingRepository();
		GroupBoardService authorService = service(authorRepository);

		authorService.deleteComment(new DeleteGroupBoardCommentCommand(USER_ID, GROUP_ID, COMMENT_ID));

		assertThat(authorRepository.comment).isNull();
		assertThat(authorRepository.lastSoftDeletedCommentAt).isEqualTo(NOW);
	}

	private static GroupBoardService service(CapturingRepository repository, UUID... ids) {
		Queue<UUID> idQueue = new ArrayDeque<>(List.of(ids));
		return new GroupBoardService(
			repository,
			CLOCK,
			() -> {
				UUID id = idQueue.poll();
				if (id == null) {
					throw new AssertionError("no deterministic id left");
				}
				return id;
			}
		);
	}

	private static GroupBoard board() {
		return GroupBoard.createDefault(BOARD_ID, GROUP_ID, GroupBoardType.NOTICE, 1, NOW);
	}

	private static GroupBoardPost post(UUID authorMemberId, boolean pinned) {
		return GroupBoardPost.create(POST_ID, GROUP_ID, BOARD_ID, authorMemberId, "원래 제목", "원래 본문", pinned, NOW);
	}

	private static GroupBoardPostSummary postSummary(UUID id, Instant createdAt) {
		return new GroupBoardPostSummary(
			id,
			GROUP_ID,
			BOARD_ID,
			MEMBER_ID,
			USER_ID,
			"현우",
			"질문",
			"본문",
			false,
			0,
			createdAt,
			createdAt
		);
	}

	private static GroupBoardComment comment(UUID authorMemberId) {
		return GroupBoardComment.create(COMMENT_ID, GROUP_ID, POST_ID, authorMemberId, "댓글", NOW);
	}

	private static GroupBoardComment comment(UUID id, UUID authorMemberId, Instant createdAt) {
		return GroupBoardComment.create(id, GROUP_ID, POST_ID, authorMemberId, "댓글", createdAt);
	}

	private static final class CapturingRepository implements GroupBoardRepository {

		private GroupBoardMembership membership = new GroupBoardMembership(
			GROUP_ID,
			MEMBER_ID,
			GroupMemberPermission.MEMBER,
			GroupMemberStatus.ACTIVE
		);
		private boolean groupExists = true;
		private List<GroupBoard> boards = new ArrayList<>();
		private List<GroupBoard> insertedBoards = List.of();
		private GroupBoardPost insertedPost;
		private GroupBoardPost post = post(MEMBER_ID, false);
		private List<GroupBoardPostSummary> postSummaries = List.of();
		private int lastPostLimit;
		private GroupBoardComment comment = comment(MEMBER_ID);
		private GroupBoardComment insertedComment;
		private List<GroupBoardComment> comments = List.of(comment(MEMBER_ID));
		private int lastCommentLimit;
		private Instant lastSoftDeletedCommentAt;

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			return groupExists;
		}

		@Override
		public Optional<GroupBoardMembership> findMembership(UUID groupId, UUID userId) {
			return Optional.ofNullable(membership);
		}

		@Override
		public List<GroupBoard> findBoardsByGroupId(UUID groupId) {
			return boards;
		}

		@Override
		public void insertDefaultBoards(List<GroupBoard> boards) {
			insertedBoards = List.copyOf(boards);
			this.boards = List.copyOf(boards);
		}

		@Override
		public Optional<GroupBoard> findBoard(UUID groupId, UUID boardId) {
			return boards.stream().filter(board -> board.id().equals(boardId)).findFirst();
		}

		@Override
		public boolean existsBoard(UUID boardId) {
			return boards.stream().anyMatch(board -> board.id().equals(boardId));
		}

		@Override
		public boolean insertPost(GroupBoardPost post) {
			insertedPost = post;
			this.post = post;
			return true;
		}

		@Override
		public List<GroupBoardPostSummary> findPosts(UUID groupId, UUID boardId, GroupBoardPostCursor cursor, int limit) {
			lastPostLimit = limit;
			return postSummaries;
		}

		@Override
		public Optional<GroupBoardPost> findPost(UUID groupId, UUID postId) {
			return Optional.ofNullable(post);
		}

		@Override
		public boolean existsPost(UUID postId) {
			return post != null && post.id().equals(postId);
		}

		@Override
		public boolean updatePost(GroupBoardPost post) {
			this.post = post;
			return true;
		}

		@Override
		public boolean softDeletePost(UUID groupId, UUID postId, Instant deletedAt) {
			post = null;
			return true;
		}

		@Override
		public boolean insertComment(GroupBoardComment comment) {
			insertedComment = comment;
			this.comment = comment;
			return true;
		}

		@Override
		public List<GroupBoardComment> findComments(UUID groupId, UUID postId, GroupBoardCommentCursor cursor, int limit) {
			lastCommentLimit = limit;
			return comments;
		}

		@Override
		public Optional<GroupBoardComment> findComment(UUID groupId, UUID commentId) {
			return Optional.ofNullable(comment);
		}

		@Override
		public boolean existsComment(UUID commentId) {
			return comment != null && comment.id().equals(commentId);
		}

		@Override
		public boolean updateComment(GroupBoardComment comment) {
			this.comment = comment;
			return true;
		}

		@Override
		public boolean softDeleteComment(UUID groupId, UUID commentId, Instant deletedAt) {
			lastSoftDeletedCommentAt = deletedAt;
			comment = null;
			return true;
		}
	}
}
