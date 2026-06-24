package com.studypot.aistudyleader.ai.service;

import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPost;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardType;
import com.studypot.aistudyleader.studygroup.board.service.CreateGroupBoardPostCommand;
import com.studypot.aistudyleader.studygroup.board.service.GroupBoardService;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;

/**
 * {@link AiConversationBoardGateway} 어댑터: 그룹 게시판 서비스의 QUESTION 보드에 글을 작성한다.
 * GroupBoardService 를 런타임에 ObjectProvider 로 resolve 한다(빈 등록 순서/조건과 무관하게 안전).
 * 보드가 없으면 생성(findOrCreateBoardId)하고, 작성 권한/멤버십 검증은 GroupBoardService 가 수행한다.
 */
class GroupBoardBackedAiConversationBoardGateway implements AiConversationBoardGateway {

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
			new CreateGroupBoardPostCommand(authenticatedUserId, groupId, boardId, title, content, false)
		);
		return post.id();
	}
}
