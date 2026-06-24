package com.studypot.aistudyleader.ai.service;

import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPost;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardType;
import com.studypot.aistudyleader.studygroup.board.service.CreateGroupBoardPostCommand;
import com.studypot.aistudyleader.studygroup.board.service.GroupBoardService;
import java.util.Objects;
import java.util.UUID;

/**
 * {@link AiConversationBoardGateway} 어댑터: 그룹 게시판 서비스의 QUESTION 보드에 글을 작성한다.
 * 보드가 없으면 생성(findOrCreateBoardId)하고, 작성 권한/멤버십 검증은 GroupBoardService 가 수행한다.
 */
class GroupBoardBackedAiConversationBoardGateway implements AiConversationBoardGateway {

	private final GroupBoardService groupBoardService;

	GroupBoardBackedAiConversationBoardGateway(GroupBoardService groupBoardService) {
		this.groupBoardService = Objects.requireNonNull(groupBoardService, "groupBoardService must not be null");
	}

	@Override
	public UUID shareQuestionToBoard(UUID authenticatedUserId, UUID groupId, String title, String content) {
		UUID boardId = groupBoardService.findOrCreateBoardId(authenticatedUserId, groupId, GroupBoardType.QUESTION);
		GroupBoardPost post = groupBoardService.createPost(
			new CreateGroupBoardPostCommand(authenticatedUserId, groupId, boardId, title, content, false)
		);
		return post.id();
	}
}
