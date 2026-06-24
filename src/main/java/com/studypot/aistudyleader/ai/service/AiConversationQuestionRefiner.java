package com.studypot.aistudyleader.ai.service;

import java.util.UUID;

/**
 * 사용자가 '기타'로 준 지시에 맞춰 공유할 질문 글(제목/본문)을 LLM 으로 다시 작성하는 포트.
 */
public interface AiConversationQuestionRefiner {

	RefinedQuestionPost refine(
		UUID authenticatedUserId,
		UUID groupId,
		String originalTitle,
		String originalSummary,
		String instruction
	);
}
