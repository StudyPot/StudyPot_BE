package com.studypot.aistudyleader.ai.service;

import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPost;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardType;
import com.studypot.aistudyleader.studygroup.board.service.CreateGroupBoardPostCommand;
import com.studypot.aistudyleader.studygroup.board.service.DeleteGroupBoardPostCommand;
import com.studypot.aistudyleader.studygroup.board.service.GroupBoardService;
import com.studypot.aistudyleader.studygroup.board.service.UpdateGroupBoardPostCommand;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;

/**
 * {@link AiConversationBoardGateway} 어댑터: 그룹 게시판 서비스의 QUESTION 보드에 글을 작성한다.
 * GroupBoardService 를 런타임에 ObjectProvider 로 resolve 한다(빈 등록 순서/조건과 무관하게 안전).
 * 보드가 없으면 생성(findOrCreateBoardId)하고, 작성 권한/멤버십 검증은 GroupBoardService 가 수행한다.
 */
class GroupBoardBackedAiConversationBoardGateway implements AiConversationBoardGateway {

	private static final String AI_AUTHOR_DISPLAY_NAME = "AI 팀장";

	private final ObjectProvider<GroupBoardService> groupBoardService;

	GroupBoardBackedAiConversationBoardGateway(ObjectProvider<GroupBoardService> groupBoardService) {
		this.groupBoardService = Objects.requireNonNull(groupBoardService, "groupBoardService must not be null");
	}

	@Override
	public UUID shareQuestionToBoard(UUID authenticatedUserId, UUID groupId, String title, String content) {
		GroupBoardService service = groupBoardService.getIfAvailable();
		if (service == null) {
			throw new AiConversationServiceUnavailableException("group board service is not available.");
		}
		UUID boardId = service.findOrCreateBoardId(authenticatedUserId, groupId, GroupBoardType.QUESTION);
		GroupBoardPost post = service.createPost(
			new CreateGroupBoardPostCommand(authenticatedUserId, groupId, boardId, title, content, false, AI_AUTHOR_DISPLAY_NAME)
		);
		return post.id();
	}

	@Override
	public void updatePostOnBoard(UUID authenticatedUserId, UUID groupId, UUID postId, String title, String content) {
		requireService().updatePost(new UpdateGroupBoardPostCommand(authenticatedUserId, groupId, postId, title, content, null));
	}

	@Override
	public void deletePostOnBoard(UUID authenticatedUserId, UUID groupId, UUID postId) {
		requireService().deletePost(new DeleteGroupBoardPostCommand(authenticatedUserId, groupId, postId));
	}

	private GroupBoardService requireService() {
		GroupBoardService service = groupBoardService.getIfAvailable();
		if (service == null) {
			throw new AiConversationServiceUnavailableException("group board service is not available.");
		}
		return service;
	}
}
