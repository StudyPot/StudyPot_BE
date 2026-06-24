package com.studypot.aistudyleader.studygroup.board.repository;

import com.studypot.aistudyleader.studygroup.board.domain.GroupBoard;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardComment;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardCommentCursor;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardMembership;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPost;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPostCursor;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPostSummary;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPostSort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupBoardRepository {

	boolean existsStudyGroup(UUID groupId);

	/** 그룹 상태 문자열(ACTIVE/COMPLETED 등)을 조회한다. 완료 그룹 쓰기 차단용. */
	Optional<String> findGroupStatus(UUID groupId);

	Optional<GroupBoardMembership> findMembership(UUID groupId, UUID userId);

	List<GroupBoard> findBoardsByGroupId(UUID groupId);

	void insertDefaultBoards(List<GroupBoard> boards);

	Optional<GroupBoard> findBoard(UUID groupId, UUID boardId);

	boolean existsBoard(UUID boardId);

	boolean insertPost(GroupBoardPost post);

	List<GroupBoardPostSummary> findPosts(UUID groupId, UUID boardId, GroupBoardPostCursor cursor, GroupBoardPostSort sort, int limit);

	List<GroupBoardPostSummary> findAllPosts(UUID groupId, GroupBoardPostCursor cursor, GroupBoardPostSort sort, int limit);

	Optional<GroupBoardPost> findPost(UUID groupId, UUID postId);

	boolean existsPost(UUID postId);

	boolean updatePost(GroupBoardPost post);

	boolean softDeletePost(UUID groupId, UUID postId, Instant deletedAt);

	boolean insertComment(GroupBoardComment comment);

	List<GroupBoardComment> findComments(UUID groupId, UUID postId, GroupBoardCommentCursor cursor, int limit);

	Optional<GroupBoardComment> findComment(UUID groupId, UUID commentId);

	boolean existsComment(UUID commentId);

	boolean updateComment(GroupBoardComment comment);

	boolean softDeleteComment(UUID groupId, UUID commentId, Instant deletedAt);
}
