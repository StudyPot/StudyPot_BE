package com.studypot.aistudyleader.studygroup.board.service;

import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPostSort;

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
import com.studypot.aistudyleader.notification.service.NotificationEventPublisher;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.EnumSet;
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
	private final NotificationEventPublisher notificationEvents;

	public GroupBoardService(GroupBoardRepository repository, Clock clock, Supplier<UUID> idGenerator) {
		this(repository, clock, idGenerator, NotificationEventPublisher.noop());
	}

	public GroupBoardService(
		GroupBoardRepository repository,
		Clock clock,
		Supplier<UUID> idGenerator,
		NotificationEventPublisher notificationEvents
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
		this.notificationEvents = Objects.requireNonNull(notificationEvents, "notificationEvents must not be null");
	}

	@Transactional
	public List<GroupBoard> listBoards(ListGroupBoardsQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		requireActiveMembership(query.groupId(), query.authenticatedUserId());
		List<GroupBoard> boards = repository.findBoardsByGroupId(query.groupId());
		// 누락된 기본 보드 타입(나중에 추가된 LEADER_REPORT 등)을 채워 넣는다. self-heal·멱등 →
		// 기능 추가 이전에 생성된 기존 그룹도 다음 진입 시 새 기본 보드(예: AI 팀장 탭)가 자동 노출된다.
		EnumSet<GroupBoardType> existingTypes = EnumSet.noneOf(GroupBoardType.class);
		boards.forEach(board -> existingTypes.add(board.boardType()));
		List<GroupBoard> missing = defaultBoards(query.groupId()).stream()
			.filter(board -> !existingTypes.contains(board.boardType()))
			.toList();
		if (missing.isEmpty()) {
			return boards;
		}
		repository.insertDefaultBoards(missing);
		List<GroupBoard> reloaded = repository.findBoardsByGroupId(query.groupId());
		return reloaded.isEmpty() ? missing : reloaded;
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<GroupBoardPostSummary> listPosts(ListGroupBoardPostsQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		requireActiveMembership(query.groupId(), query.authenticatedUserId());
		requireBoard(query.groupId(), query.boardId());
		GroupBoardPostSort sort = query.sortOrder();
		// keyset 커서는 기본 정렬(createdAt desc)에서만 안정적. 그 외 정렬은 커서 무시 + 단일 페이지.
		GroupBoardPostCursor cursor = sort.keysetCursorSupported() ? decodePostCursor(query.cursor()) : null;
		List<GroupBoardPostSummary> fetched = repository.findPosts(query.groupId(), query.boardId(), cursor, sort, query.pageSize() + 1);
		if (fetched.size() <= query.pageSize()) {
			return CursorPageResponse.firstPage(fetched, null);
		}
		List<GroupBoardPostSummary> items = List.copyOf(fetched.subList(0, query.pageSize()));
		String nextCursor = sort.keysetCursorSupported() ? encodePostCursor(items.getLast()) : null;
		return CursorPageResponse.firstPage(items, nextCursor);
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<GroupBoardPostSummary> listAllPosts(ListAllGroupBoardPostsQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		requireActiveMembership(query.groupId(), query.authenticatedUserId());
		GroupBoardPostSort sort = query.sortOrder();
		GroupBoardPostCursor cursor = sort.keysetCursorSupported() ? decodePostCursor(query.cursor()) : null;
		List<GroupBoardPostSummary> fetched = repository.findAllPosts(query.groupId(), cursor, sort, query.pageSize() + 1);
		if (fetched.size() <= query.pageSize()) {
			return CursorPageResponse.firstPage(fetched, null);
		}
		List<GroupBoardPostSummary> items = List.copyOf(fetched.subList(0, query.pageSize()));
		String nextCursor = sort.keysetCursorSupported() ? encodePostCursor(items.getLast()) : null;
		return CursorPageResponse.firstPage(items, nextCursor);
	}

	@Transactional
	public GroupBoardPost createPost(CreateGroupBoardPostCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		GroupBoardMembership membership = requireActiveMembership(command.groupId(), command.authenticatedUserId());
		GroupBoard board = requireBoard(command.groupId(), command.boardId());
		// 완료된 스터디는 멤버 글쓰기 차단. 단 팀장 리포트(마감/수료 리포트)는 시스템이 계속 게시해야 하므로 예외.
		if (board.boardType() != GroupBoardType.LEADER_REPORT) {
			requireWritableGroup(command.groupId());
		}
		if (command.pinned() && !membership.owner()) {
			throw new GroupBoardAccessDeniedException("only the study group owner can pin board posts.");
		}
		if (board.boardType() == GroupBoardType.LEADER_REPORT && !membership.owner()) {
			throw new GroupBoardAccessDeniedException("only the study group owner can post to the leader report board.");
		}
		Instant now = clock.instant();
		String overrideName = command.authorDisplayNameOverride();
		boolean hasOverride = overrideName != null && !overrideName.isBlank();
		String displayName = hasOverride ? overrideName : membership.displayName();
		GroupBoardPost post;
		try {
			post = GroupBoardPost.create(
				idGenerator.get(),
				command.groupId(),
				command.boardId(),
				membership.memberId(),
				membership.userId(),
				displayName,
				command.title(),
				command.content(),
				command.pinned(),
				now
			).withAuthorDisplayNameOverride(hasOverride ? overrideName : null);
		} catch (IllegalArgumentException exception) {
			throw invalidRequest(fieldFromMessage(exception.getMessage()), exception);
		}
		if (!repository.insertPost(post)) {
			throw new GroupBoardMutationRejectedException("group board post could not be inserted.");
		}
		if (board.boardType() == GroupBoardType.NOTICE) {
			notificationEvents.publishNoticePosted(command.groupId(), membership.userId(), post.id(), post.title());
		} else if (board.boardType() == GroupBoardType.LEADER_REPORT) {
			notificationEvents.publishLeaderReportPosted(command.groupId(), post.id(), post.title());
		}
		return post;
	}

	/**
	 * 지정한 보드 타입의 보드 id 를 찾고, 없으면(기존 그룹) 새로 만들어 반환한다.
	 * 스케줄러가 팀장 리포트 보드(LEADER_REPORT)에 게시할 때 사용한다.
	 */
	@Transactional
	public UUID findOrCreateBoardId(UUID authenticatedUserId, UUID groupId, GroupBoardType boardType) {
		Objects.requireNonNull(boardType, "boardType must not be null");
		List<GroupBoard> boards = listBoards(new ListGroupBoardsQuery(authenticatedUserId, groupId));
		return boards.stream()
			.filter(board -> board.boardType() == boardType)
			.map(GroupBoard::id)
			.findFirst()
			.orElseGet(() -> {
				GroupBoard board = GroupBoard.createDefault(idGenerator.get(), groupId, boardType, boards.size() + 1, clock.instant());
				repository.insertDefaultBoards(List.of(board));
				return board.id();
			});
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
		requireWritableGroup(command.groupId());
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
		requireWritableGroup(command.groupId());
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
		requireWritableGroup(command.groupId());
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
		requireWritableGroup(command.groupId());
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
		requireWritableGroup(command.groupId());
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

	private void requireWritableGroup(UUID groupId) {
		String status = repository.findGroupStatus(groupId).orElse(null);
		if ("COMPLETED".equals(status) || "ARCHIVED".equals(status)) {
			throw new GroupBoardAccessDeniedException("완료된 스터디의 게시판에는 새 글/댓글을 쓸 수 없어요.");
		}
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
			return new GroupBoardPostCursor(parseCursorBoolean(parts[0]), Instant.parse(parts[1]), UUID.fromString(parts[2]));
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

	private static boolean parseCursorBoolean(String value) {
		if (!"true".equals(value) && !"false".equals(value)) {
			throw new IllegalArgumentException("cursor boolean is invalid.");
		}
		return Boolean.parseBoolean(value);
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
