package com.studypot.aistudyleader.studygroup.board.service;

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
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.annotation.Transactional;

public class GroupBoardService {

	private static final Base64.Encoder CURSOR_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder CURSOR_DECODER = Base64.getUrlDecoder();

	private final GroupBoardRepository repository;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;

	public GroupBoardService(GroupBoardRepository repository, Clock clock, Supplier<UUID> idGenerator) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
	}

	@Transactional
	public List<GroupBoard> listBoards(ListGroupBoardsQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		requireActiveMembership(query.groupId(), query.authenticatedUserId());
		List<GroupBoard> boards = repository.findBoardsByGroupId(query.groupId());
		if (!boards.isEmpty()) {
			return boards;
		}
		List<GroupBoard> defaults = defaultBoards(query.groupId());
		repository.insertDefaultBoards(defaults);
		List<GroupBoard> reloaded = repository.findBoardsByGroupId(query.groupId());
		return reloaded.isEmpty() ? defaults : reloaded;
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<GroupBoardPostSummary> listPosts(ListGroupBoardPostsQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		requireActiveMembership(query.groupId(), query.authenticatedUserId());
		requireBoard(query.groupId(), query.boardId());
		GroupBoardPostCursor cursor = decodePostCursor(query.cursor());
		List<GroupBoardPostSummary> fetched = repository.findPosts(query.groupId(), query.boardId(), cursor, query.pageSize() + 1);
		if (fetched.size() <= query.pageSize()) {
			return CursorPageResponse.firstPage(fetched, null);
		}
		List<GroupBoardPostSummary> items = List.copyOf(fetched.subList(0, query.pageSize()));
		return CursorPageResponse.firstPage(items, encodePostCursor(items.getLast()));
	}

	@Transactional
	public GroupBoardPost createPost(CreateGroupBoardPostCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		GroupBoardMembership membership = requireActiveMembership(command.groupId(), command.authenticatedUserId());
		requireBoard(command.groupId(), command.boardId());
		if (command.pinned() && !membership.owner()) {
			throw new GroupBoardAccessDeniedException("only the study group owner can pin board posts.");
		}
		Instant now = clock.instant();
		GroupBoardPost post;
		try {
			post = GroupBoardPost.create(
				idGenerator.get(),
				command.groupId(),
				command.boardId(),
				membership.memberId(),
				membership.userId(),
				membership.displayName(),
				command.title(),
				command.content(),
				command.pinned(),
				now
			);
		} catch (IllegalArgumentException exception) {
			throw invalidRequest(fieldFromMessage(exception.getMessage()), exception);
		}
		if (!repository.insertPost(post)) {
			throw new GroupBoardMutationRejectedException("group board post could not be inserted.");
		}
		return post;
	}

	@Transactional(readOnly = true)
	public GroupBoardPost getPost(GetGroupBoardPostQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		requireActiveMembership(query.groupId(), query.authenticatedUserId());
		return requirePost(query.groupId(), query.postId());
	}

	@Transactional
	public GroupBoardPost updatePost(UpdateGroupBoardPostCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		GroupBoardMembership membership = requireActiveMembership(command.groupId(), command.authenticatedUserId());
		GroupBoardPost post = requirePost(command.groupId(), command.postId());
		if (command.changesContent() && !post.authoredBy(membership.memberId())) {
			throw new GroupBoardAccessDeniedException("only the post author can update post title or content.");
		}
		if (command.pinned() != null && !membership.owner()) {
			throw new GroupBoardAccessDeniedException("only the study group owner can pin board posts.");
		}
		GroupBoardPost updated;
		try {
			updated = post.update(command.title(), command.content(), command.pinned(), clock.instant());
		} catch (IllegalArgumentException exception) {
			throw invalidRequest(fieldFromMessage(exception.getMessage()), exception);
		}
		if (!repository.updatePost(updated)) {
			throw new GroupBoardMutationRejectedException("group board post could not be updated.");
		}
		return updated;
	}

	@Transactional
	public void deletePost(DeleteGroupBoardPostCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		GroupBoardMembership membership = requireActiveMembership(command.groupId(), command.authenticatedUserId());
		GroupBoardPost post = requirePost(command.groupId(), command.postId());
		if (!post.authoredBy(membership.memberId()) && !membership.owner()) {
			throw new GroupBoardAccessDeniedException("only the post author or study group owner can delete board posts.");
		}
		if (!repository.softDeletePost(command.groupId(), command.postId(), clock.instant())) {
			throw new GroupBoardMutationRejectedException("group board post could not be deleted.");
		}
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<GroupBoardComment> listComments(ListGroupBoardCommentsQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		requireActiveMembership(query.groupId(), query.authenticatedUserId());
		requirePost(query.groupId(), query.postId());
		GroupBoardCommentCursor cursor = decodeCommentCursor(query.cursor());
		List<GroupBoardComment> fetched = repository.findComments(query.groupId(), query.postId(), cursor, query.pageSize() + 1);
		if (fetched.size() <= query.pageSize()) {
			return CursorPageResponse.firstPage(fetched, null);
		}
		List<GroupBoardComment> items = List.copyOf(fetched.subList(0, query.pageSize()));
		return CursorPageResponse.firstPage(items, encodeCommentCursor(items.getLast()));
	}

	@Transactional
	public GroupBoardComment createComment(CreateGroupBoardCommentCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		GroupBoardMembership membership = requireActiveMembership(command.groupId(), command.authenticatedUserId());
		requirePost(command.groupId(), command.postId());
		Instant now = clock.instant();
		GroupBoardComment comment;
		try {
			comment = GroupBoardComment.create(
				idGenerator.get(),
				command.groupId(),
				command.postId(),
				membership.memberId(),
				membership.userId(),
				membership.displayName(),
				command.content(),
				now
			);
		} catch (IllegalArgumentException exception) {
			throw invalidRequest("content", exception);
		}
		if (!repository.insertComment(comment)) {
			throw new GroupBoardMutationRejectedException("group board comment could not be inserted.");
		}
		return comment;
	}

	@Transactional
	public GroupBoardComment updateComment(UpdateGroupBoardCommentCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		GroupBoardMembership membership = requireActiveMembership(command.groupId(), command.authenticatedUserId());
		GroupBoardComment comment = requireComment(command.groupId(), command.commentId());
		if (!comment.authoredBy(membership.memberId())) {
			throw new GroupBoardAccessDeniedException("only the comment author can update comment content.");
		}
		GroupBoardComment updated;
		try {
			updated = comment.update(command.content(), clock.instant());
		} catch (IllegalArgumentException exception) {
			throw invalidRequest("content", exception);
		}
		if (!repository.updateComment(updated)) {
			throw new GroupBoardMutationRejectedException("group board comment could not be updated.");
		}
		return updated;
	}

	@Transactional
	public void deleteComment(DeleteGroupBoardCommentCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		GroupBoardMembership membership = requireActiveMembership(command.groupId(), command.authenticatedUserId());
		GroupBoardComment comment = requireComment(command.groupId(), command.commentId());
		if (!comment.authoredBy(membership.memberId()) && !membership.owner()) {
			throw new GroupBoardAccessDeniedException("only the comment author or study group owner can delete board comments.");
		}
		if (!repository.softDeleteComment(command.groupId(), command.commentId(), clock.instant())) {
			throw new GroupBoardMutationRejectedException("group board comment could not be deleted.");
		}
	}

	private List<GroupBoard> defaultBoards(UUID groupId) {
		Instant now = clock.instant();
		GroupBoardType[] types = GroupBoardType.values();
		return java.util.stream.IntStream.range(0, types.length)
			.mapToObj(index -> GroupBoard.createDefault(idGenerator.get(), groupId, types[index], index + 1, now))
			.toList();
	}

	private GroupBoardMembership requireActiveMembership(UUID groupId, UUID userId) {
		GroupBoardMembership membership = repository.findMembership(groupId, userId)
			.orElseGet(() -> {
				if (!repository.existsStudyGroup(groupId)) {
					throw new GroupBoardNotFoundException("study group was not found.");
				}
				throw new GroupBoardAccessDeniedException("authenticated user is not a member of this study group.");
			});
		if (!membership.active()) {
			throw new GroupBoardAccessDeniedException("active group membership is required for group board access.");
		}
		return membership;
	}

	private GroupBoard requireBoard(UUID groupId, UUID boardId) {
		return repository.findBoard(groupId, boardId)
			.orElseGet(() -> {
				if (repository.existsBoard(boardId)) {
					throw new GroupBoardAccessDeniedException("group board does not belong to this study group.");
				}
				throw new GroupBoardNotFoundException("group board was not found.");
			});
	}

	private GroupBoardPost requirePost(UUID groupId, UUID postId) {
		return repository.findPost(groupId, postId)
			.orElseGet(() -> {
				if (repository.existsPost(postId)) {
					throw new GroupBoardAccessDeniedException("group board post does not belong to this study group.");
				}
				throw new GroupBoardNotFoundException("group board post was not found.");
			});
	}

	private GroupBoardComment requireComment(UUID groupId, UUID commentId) {
		return repository.findComment(groupId, commentId)
			.orElseGet(() -> {
				if (repository.existsComment(commentId)) {
					throw new GroupBoardAccessDeniedException("group board comment does not belong to this study group.");
				}
				throw new GroupBoardNotFoundException("group board comment was not found.");
			});
	}

	private static String encodePostCursor(GroupBoardPostSummary post) {
		String rawCursor = post.pinned() + "|" + post.createdAt() + "|" + post.id();
		return CURSOR_ENCODER.encodeToString(rawCursor.getBytes(StandardCharsets.UTF_8));
	}

	private static GroupBoardPostCursor decodePostCursor(String cursor) {
		if (cursor == null) {
			return null;
		}
		try {
			String decoded = new String(CURSOR_DECODER.decode(cursor), StandardCharsets.UTF_8);
			String[] parts = decoded.split("\\|", 3);
			if (parts.length != 3) {
				throw new IllegalArgumentException("cursor format is invalid.");
			}
			return new GroupBoardPostCursor(Boolean.parseBoolean(parts[0]), Instant.parse(parts[1]), UUID.fromString(parts[2]));
		} catch (IllegalArgumentException | DateTimeParseException exception) {
			throw new InvalidGroupBoardRequestException("cursor", "cursor is invalid.");
		}
	}

	private static String encodeCommentCursor(GroupBoardComment comment) {
		String rawCursor = comment.createdAt() + "|" + comment.id();
		return CURSOR_ENCODER.encodeToString(rawCursor.getBytes(StandardCharsets.UTF_8));
	}

	private static GroupBoardCommentCursor decodeCommentCursor(String cursor) {
		if (cursor == null) {
			return null;
		}
		try {
			String decoded = new String(CURSOR_DECODER.decode(cursor), StandardCharsets.UTF_8);
			String[] parts = decoded.split("\\|", 2);
			if (parts.length != 2) {
				throw new IllegalArgumentException("cursor format is invalid.");
			}
			return new GroupBoardCommentCursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
		} catch (IllegalArgumentException | DateTimeParseException exception) {
			throw new InvalidGroupBoardRequestException("cursor", "cursor is invalid.");
		}
	}

	private static InvalidGroupBoardRequestException invalidRequest(String field, IllegalArgumentException exception) {
		String message = exception.getMessage();
		return new InvalidGroupBoardRequestException(field, message == null || message.isBlank() ? "Invalid value." : message);
	}

	private static String fieldFromMessage(String message) {
		if (message != null && message.startsWith("title ")) {
			return "title";
		}
		if (message != null && message.startsWith("content ")) {
			return "content";
		}
		return "request";
	}
}
