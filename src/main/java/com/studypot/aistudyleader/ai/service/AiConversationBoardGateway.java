package com.studypot.aistudyleader.ai.service;

import java.util.UUID;

/**
 * AI 팀장 대화에서 확정된 '질문 게시판 공유' 액션을 실제 게시판 글 작성으로 위임하는 포트.
 * ai 모듈은 이 추상화에만 의존하고, 구현(어댑터)이 그룹 게시판 서비스로 연결한다.
 */
public interface AiConversationBoardGateway {

	/**
	 * 질문을 그룹의 '질문' 게시판에 작성자(요청 멤버) 명의로 등록하고 생성된 글 id 를 반환한다.
	 */
	UUID shareQuestionToBoard(UUID authenticatedUserId, UUID groupId, String title, String content);
}
